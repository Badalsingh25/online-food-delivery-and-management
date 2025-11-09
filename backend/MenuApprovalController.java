package com.hungerexpress.menu;

import com.hungerexpress.common.CurrentUser;
import com.hungerexpress.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/menu-approvals")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MenuApprovalController {

    private final MenuItemRepository menuItems;
    private final UserRepository users;
    private final MenuCategoryRepository categories;
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    record MenuItemApprovalDto(
        Long id,
        String name,
        String description,
        double price,
        String imageUrl,
        String restaurantName,
        Long restaurantId,
        String categoryName,
        String approvalStatus,
        String submittedAt,
        String submittedByName,
        String rejectionReason,
        boolean available
    ) {}

    record ApprovalRequest(String action, String reason) {}
    
    record ApprovalStats(
        long pendingCount,
        long approvedCount,
        long rejectedCount,
        long totalCount
    ) {}

    @GetMapping("/stats")
    public ResponseEntity<ApprovalStats> getStats() {
        long pending = menuItems.countByApprovalStatus(MenuItem.ApprovalStatus.PENDING);
        long approved = menuItems.countByApprovalStatus(MenuItem.ApprovalStatus.APPROVED);
        long rejected = menuItems.countByApprovalStatus(MenuItem.ApprovalStatus.REJECTED);
        long total = menuItems.count();
        
        return ResponseEntity.ok(new ApprovalStats(pending, approved, rejected, total));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<MenuItemApprovalDto>> getPendingItems() {
        List<MenuItem> pending = menuItems.findByApprovalStatusOrderBySubmittedAtDesc(MenuItem.ApprovalStatus.PENDING);
        return ResponseEntity.ok(pending.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/all")
    public ResponseEntity<List<MenuItemApprovalDto>> getAllItems(
        @RequestParam(required = false) String status
    ) {
        List<MenuItem> items;
        if (status != null && !status.isEmpty()) {
            try {
                MenuItem.ApprovalStatus approvalStatus = MenuItem.ApprovalStatus.valueOf(status.toUpperCase());
                items = menuItems.findByApprovalStatusOrderBySubmittedAtDesc(approvalStatus);
            } catch (IllegalArgumentException e) {
                items = menuItems.findAll();
            }
        } else {
            items = menuItems.findAll();
        }
        
        return ResponseEntity.ok(items.stream()
            .sorted((a, b) -> {
                if (a.getSubmittedAt() == null && b.getSubmittedAt() == null) return 0;
                if (a.getSubmittedAt() == null) return 1;
                if (b.getSubmittedAt() == null) return -1;
                return b.getSubmittedAt().compareTo(a.getSubmittedAt());
            })
            .map(this::toDto)
            .collect(Collectors.toList()));
    }

    @PostMapping("/{itemId}/approve")
    public ResponseEntity<MenuItemApprovalDto> approveItem(@PathVariable Long itemId) {
        MenuItem item = menuItems.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Menu item not found"));

        // Get current admin user ID
        String email = CurrentUser.email();
        Long adminId = null;
        if (email != null) {
            adminId = users.findByEmail(email).map(u -> u.getId()).orElse(null);
        }

        // Auto-fix category if wrong or missing
        MenuCategory currentCategory = item.getCategory();
        if (currentCategory == null || 
            currentCategory.getRestaurant() == null || 
            !currentCategory.getRestaurant().getId().equals(item.getRestaurant().getId())) {
            
            List<MenuCategory> restaurantCategories = categories.findByRestaurantOrderByPositionAsc(item.getRestaurant());
            if (!restaurantCategories.isEmpty()) {
                item.setCategory(restaurantCategories.get(0));
                System.out.println("[MenuApprovalController] ✅ Auto-fixed category for item '" + item.getName() + "' to: " + restaurantCategories.get(0).getName());
            } else {
                System.err.println("[MenuApprovalController] ⚠️ No categories found for restaurant " + item.getRestaurant().getName());
            }
        }

        item.setApprovalStatus(MenuItem.ApprovalStatus.APPROVED);
        item.setApprovedAt(Instant.now());
        item.setApprovedBy(adminId);
        item.setRejectionReason(null);
        
        MenuItem saved = menuItems.save(item);
        return ResponseEntity.ok(toDto(saved));
    }

    @PostMapping("/{itemId}/reject")
    public ResponseEntity<MenuItemApprovalDto> rejectItem(
        @PathVariable Long itemId,
        @RequestBody ApprovalRequest request
    ) {
        MenuItem item = menuItems.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Menu item not found"));

        // Get current admin user ID
        String email = CurrentUser.email();
        Long adminId = null;
        if (email != null) {
            adminId = users.findByEmail(email).map(u -> u.getId()).orElse(null);
        }

        item.setApprovalStatus(MenuItem.ApprovalStatus.REJECTED);
        item.setApprovedAt(Instant.now());
        item.setApprovedBy(adminId);
        item.setRejectionReason(request.reason());
        
        MenuItem saved = menuItems.save(item);
        return ResponseEntity.ok(toDto(saved));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long itemId) {
        menuItems.deleteById(itemId);
        return ResponseEntity.noContent().build();
    }

    private MenuItemApprovalDto toDto(MenuItem item) {
        String submittedByName = "Unknown";
        if (item.getSubmittedBy() != null) {
            submittedByName = users.findById(item.getSubmittedBy())
                .map(u -> u.getFullName() != null ? u.getFullName() : u.getEmail())
                .orElse("Unknown");
        }

        return new MenuItemApprovalDto(
            item.getId(),
            item.getName(),
            item.getDescription(),
            item.getPrice().doubleValue(),
            item.getImageUrl(),
            item.getRestaurant() != null ? item.getRestaurant().getName() : "Unknown",
            item.getRestaurant() != null ? item.getRestaurant().getId() : null,
            item.getCategory() != null ? item.getCategory().getName() : "Uncategorized",
            item.getApprovalStatus().name(),
            item.getSubmittedAt() != null ? formatter.format(item.getSubmittedAt()) : "N/A",
            submittedByName,
            item.getRejectionReason(),
            item.getAvailable() != null ? item.getAvailable() : true
        );
    }
}
