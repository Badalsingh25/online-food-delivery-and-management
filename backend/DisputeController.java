package com.hungerexpress.dispute;

import com.hungerexpress.common.CurrentUser;
import com.hungerexpress.notification.NotificationService;
import com.hungerexpress.orders.OrderEntity;
import com.hungerexpress.orders.OrderRepository;
import com.hungerexpress.user.User;
import com.hungerexpress.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class DisputeController {
    
    private final DisputeRepository disputeRepo;
    private final UserRepository userRepo;
    private final OrderRepository orderRepo;
    private final NotificationService notificationService;
    
    // Create dispute (Customer)
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<DisputeEntity> createDispute(@RequestBody CreateDisputeRequest request) {
        String email = CurrentUser.email();
        if (email == null) return ResponseEntity.status(401).build();
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        
        // Verify order belongs to user
        OrderEntity order = orderRepo.findById(request.orderId()).orElse(null);
        if (order == null || !order.getUserId().equals(user.getId())) {
            return ResponseEntity.badRequest().build();
        }
        
        DisputeEntity dispute = DisputeEntity.builder()
            .orderId(request.orderId())
            .userId(user.getId())
            .restaurantId(order.getRestaurantId())
            .type(request.type())
            .subject(request.subject())
            .description(request.description())
            .build();
        
        DisputeEntity saved = disputeRepo.save(dispute);
        
        // Notify admin
        notificationService.sendNotification(
            1L, // Admin user ID (assuming ID 1 is admin)
            com.hungerexpress.notification.NotificationEntity.NotificationType.SYSTEM_ALERT,
            "New Dispute Filed",
            "A customer has filed a dispute for Order #" + request.orderId(),
            saved.getId()
        );
        
        return ResponseEntity.ok(saved);
    }
    
    // Get user disputes
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<DisputeEntity>> getMyDisputes() {
        String email = CurrentUser.email();
        if (email == null) return ResponseEntity.status(401).build();
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        
        List<DisputeEntity> disputes = disputeRepo.findByUserIdOrderByCreatedAtDesc(user.getId());
        return ResponseEntity.ok(disputes);
    }
    
    // Get dispute by ID
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DisputeEntity> getDispute(@PathVariable Long id) {
        String email = CurrentUser.email();
        if (email == null) return ResponseEntity.status(401).build();
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        
        DisputeEntity dispute = disputeRepo.findById(id).orElse(null);
        if (dispute == null) return ResponseEntity.notFound().build();
        
        // Check access rights
        boolean isAdmin = "ADMIN".equals(user.getRole());
        boolean isOwner = dispute.getUserId().equals(user.getId());
        
        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(dispute);
    }
    
    // Admin: Get all disputes
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DisputeEntity>> getAllDisputes(
        @RequestParam(required = false) DisputeEntity.DisputeStatus status
    ) {
        List<DisputeEntity> disputes;
        
        if (status != null) {
            disputes = disputeRepo.findByStatusOrderByCreatedAtDesc(status);
        } else {
            disputes = disputeRepo.findAll();
        }
        
        return ResponseEntity.ok(disputes);
    }
    
    // Admin: Get dispute statistics
    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DisputeStats> getDisputeStats() {
        Long openCount = disputeRepo.countByStatus(DisputeEntity.DisputeStatus.OPEN);
        Long inReviewCount = disputeRepo.countByStatus(DisputeEntity.DisputeStatus.IN_REVIEW);
        Long resolvedCount = disputeRepo.countByStatus(DisputeEntity.DisputeStatus.RESOLVED);
        Long rejectedCount = disputeRepo.countByStatus(DisputeEntity.DisputeStatus.REJECTED);
        
        DisputeStats stats = new DisputeStats(
            openCount,
            inReviewCount,
            resolvedCount,
            rejectedCount,
            openCount + inReviewCount + resolvedCount + rejectedCount
        );
        
        return ResponseEntity.ok(stats);
    }
    
    // Admin: Resolve dispute
    @PutMapping("/admin/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DisputeEntity> resolveDispute(
        @PathVariable Long id,
        @RequestBody ResolveDisputeRequest request
    ) {
        String email = CurrentUser.email();
        if (email == null) return ResponseEntity.status(401).build();
        
        User admin = userRepo.findByEmail(email).orElse(null);
        if (admin == null) return ResponseEntity.notFound().build();
        
        DisputeEntity dispute = disputeRepo.findById(id).orElse(null);
        if (dispute == null) return ResponseEntity.notFound().build();
        
        dispute.setStatus(request.approved() ? DisputeEntity.DisputeStatus.RESOLVED : DisputeEntity.DisputeStatus.REJECTED);
        dispute.setAdminResponse(request.response());
        dispute.setResolvedBy(admin.getId());
        dispute.setResolvedAt(Instant.now());
        dispute.setUpdatedAt(Instant.now());
        
        if (request.refundAmount() != null) {
            dispute.setRefundAmount(request.refundAmount());
        }
        
        DisputeEntity saved = disputeRepo.save(dispute);
        
        // Notify customer
        notificationService.sendNotification(
            dispute.getUserId(),
            com.hungerexpress.notification.NotificationEntity.NotificationType.SYSTEM_ALERT,
            "Dispute " + (request.approved() ? "Resolved" : "Rejected"),
            "Your dispute for Order #" + dispute.getOrderId() + " has been " + 
                (request.approved() ? "resolved" : "rejected") + ". " + request.response(),
            dispute.getId()
        );
        
        return ResponseEntity.ok(saved);
    }
    
    // Admin: Update dispute status
    @PutMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DisputeEntity> updateDisputeStatus(
        @PathVariable Long id,
        @RequestBody UpdateStatusRequest request
    ) {
        DisputeEntity dispute = disputeRepo.findById(id).orElse(null);
        if (dispute == null) return ResponseEntity.notFound().build();
        
        dispute.setStatus(request.status());
        dispute.setUpdatedAt(Instant.now());
        
        DisputeEntity saved = disputeRepo.save(dispute);
        return ResponseEntity.ok(saved);
    }
    
    // DTOs
    record CreateDisputeRequest(
        Long orderId,
        DisputeEntity.DisputeType type,
        String subject,
        String description
    ) {}
    
    record ResolveDisputeRequest(
        Boolean approved,
        String response,
        java.math.BigDecimal refundAmount
    ) {}
    
    record UpdateStatusRequest(
        DisputeEntity.DisputeStatus status
    ) {}
    
    record DisputeStats(
        Long openCount,
        Long inReviewCount,
        Long resolvedCount,
        Long rejectedCount,
        Long totalCount
    ) {}
}
