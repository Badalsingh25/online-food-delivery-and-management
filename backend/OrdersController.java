package com.hungerexpress.orders;

import com.hungerexpress.agent.AgentAssignmentService;
import com.hungerexpress.agent.AgentOrderAssignment;
import com.hungerexpress.agent.AgentOrderRepository;
import com.hungerexpress.cart.CartItemDto;
import com.hungerexpress.cart.CartStore;
import com.hungerexpress.common.CurrentUser;
import com.hungerexpress.payments.PaymentEntity;
import com.hungerexpress.payments.PaymentRepository;
import com.hungerexpress.user.User;
import com.hungerexpress.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.hungerexpress.coupons.CouponRepository;
import com.hungerexpress.coupons.CouponEntity;
import java.time.Instant;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrdersController {

    private final CartStore cartStore;
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final UserRepository users;
    private final CouponRepository coupons;
    private final AgentAssignmentService agentAssignmentService;
    private final AgentOrderRepository agentOrderRepository;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    static OrderSummaryDto toDto(OrderEntity e){
        return new OrderSummaryDto(
                e.getId(),
                e.getStatus().name(),
                e.getTotal().doubleValue(),
                e.getCreatedAt().toEpochMilli(),
                e.getItems().stream().map(i -> new OrderItemDto(i.getId(), i.getName(), i.getPrice().doubleValue(), i.getQty())).collect(Collectors.toList())
        );
    }

    // Agent: directly mark delivered (simple flow)
    @PreAuthorize("hasRole('AGENT')")
    @PatchMapping("/{id}/deliver")
    public ResponseEntity<OrderSummaryDto> deliver(@PathVariable Long id){
        Long uid = currentUserId();
        if (uid == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        Optional<OrderEntity> orderOpt = orders.findByIdWithItems(id);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        OrderEntity o = orderOpt.get();
        if (o.getAssignedTo() == null || !o.getAssignedTo().equals(uid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        o.setStatus(OrderStatus.DELIVERED);
        o.setDeliveredAt(Instant.now());
        orders.save(o);
        
        AgentOrderAssignment a = agentOrderRepository.findTopByOrderIdOrderByAssignedAtDesc(o.getId());
        if (a != null) { 
            a.setDeliveredAt(Instant.now()); 
            a.setStatus("DELIVERED"); 
            agentOrderRepository.save(a); 
        }
        publishUpdate();
        return ResponseEntity.ok(toDto(o));
    }

    private Long currentUserId(){
        String email = Optional.ofNullable(CurrentUser.email()).orElse(null);
        if (email == null) return null;
        Optional<User> u = users.findByEmail(email);
        return u.map(User::getId).orElse(null);
    }

    record Address(String name, String phone, String line1, String line2, String city, String state, String postal, String country){}
    record OrderItemRequest(Long id, String name, double price, int qty){}
    record CreateOrderRequest(String providerOrderId, String couponCode, Address address, List<OrderItemRequest> items){}

    @PostMapping
    public ResponseEntity<OrderSummaryDto> create(@RequestBody(required = false) CreateOrderRequest req){
        System.out.println("🛒 ====== CREATE ORDER START ======");
        System.out.println("📦 Request body: " + (req != null ? "present" : "null"));
        
        Long uid = currentUserId();
        System.out.println("👤 User ID: " + uid);
        
        // IMPORTANT: Guest orders are allowed, but if a JWT token is present, always use it
        if (uid != null) {
            System.out.println("✅ Authenticated user detected - Order will be linked to userId: " + uid);
        } else {
            System.out.println("⚠️ WARNING: No userId found - Creating GUEST order (user_id = NULL)");
        }
        
        // Use items from request if provided, otherwise use server cart
        List<CartItemDto> items;
        if (req != null && req.items != null && !req.items.isEmpty()) {
            System.out.println("✅ Using items from request body: " + req.items.size() + " items");
            items = req.items.stream()
                .map(i -> new CartItemDto(i.id, i.name, i.price, null, i.qty))
                .toList();
        } else {
            System.out.println("⚠️  No items in request, checking cart store...");
            items = new ArrayList<>(cartStore.get(Optional.ofNullable(CurrentUser.email()).orElse("guest")));
            System.out.println("📋 Cart store items: " + items.size());
        }
        
        if (items.isEmpty()) {
            System.out.println("❌ No items to order!");
            return ResponseEntity.badRequest().build();
        }

        BigDecimal subtotal = items.stream().map(i -> BigDecimal.valueOf(i.price()).multiply(BigDecimal.valueOf(i.qty()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = BigDecimal.ZERO;
        String appliedCode = null;
        if (req != null && req.couponCode != null && !req.couponCode.isBlank()){
            Optional<CouponEntity> c = coupons.findByCodeAndActiveIsTrue(req.couponCode.toUpperCase());
            if (c.isPresent()){
                CouponEntity cc = c.get();
                if ((cc.getExpiresAt() == null || !cc.getExpiresAt().isBefore(Instant.now()))
                    && (cc.getMinAmount() == null || subtotal.compareTo(cc.getMinAmount()) >= 0)){
                    if (cc.getPercentOff() != null) discount = discount.add(subtotal.multiply(BigDecimal.valueOf(cc.getPercentOff()).movePointLeft(2)));
                    if (cc.getAmountOff() != null) discount = discount.add(cc.getAmountOff());
                    if (discount.compareTo(subtotal) > 0) discount = subtotal;
                    appliedCode = cc.getCode();
                }
            }
        }

        OrderEntity e = OrderEntity.builder()
                .userId(uid)
                .status(OrderStatus.PLACED)
                .subtotal(subtotal)
                .discount(discount)
                .couponCode(appliedCode)
                .tax(BigDecimal.ZERO)
                .deliveryFee(BigDecimal.ZERO)
                .total(subtotal.subtract(discount))
                .build();
        if (req != null && req.address != null){
            e.setShipName(req.address.name());
            e.setShipPhone(req.address.phone());
            e.setShipLine1(req.address.line1());
            e.setShipLine2(req.address.line2());
            e.setShipCity(req.address.city());
            e.setShipState(req.address.state());
            e.setShipPostal(req.address.postal());
            e.setShipCountry(req.address.country());
        }
        e.setPlacedAt(Instant.now());
        List<OrderItemEntity> its = items.stream().map(i -> OrderItemEntity.builder()
                .order(e)
                .menuItemId(i.id())
                .name(i.name())
                .price(BigDecimal.valueOf(i.price()))
                .qty(i.qty())
                .build()).collect(Collectors.toList());
        e.setItems(its);
        
        System.out.println("💾 Saving order with " + its.size() + " items...");
        System.out.println("📌 Order will be saved with userId: " + uid);
        OrderEntity saved = orders.save(e);
        System.out.println("✅ Order saved! ID: " + saved.getId() + " | userId: " + saved.getUserId());
        
        publishUpdate();
        
        // Link payment to order if providerOrderId present
        if (req != null && req.providerOrderId != null) {
            payments.findByProviderOrderId(req.providerOrderId).ifPresent(p -> {
                p.setOrder(e);
                p.setStatus("AUTHORIZED");
                payments.save(p);
            });
        }

        cartStore.clear(Optional.ofNullable(CurrentUser.email()).orElse("guest"));
        return ResponseEntity.ok(toDto(e));
    }

    @GetMapping
    public ResponseEntity<List<OrderSummaryDto>> list(@RequestParam(required = false) String view){
        Long uid = currentUserId();
        System.out.println("📋 Listing orders for user ID: " + uid);
        if (uid == null) return ResponseEntity.status(401).build();
        
        // If view=all and user is owner/admin, return all orders for kanban board
        if ("all".equals(view)) {
            // TODO: Add proper role check (OWNER/ADMIN) when security is fully configured
            // For now, allow any authenticated user to view all orders
            List<OrderSummaryDto> res = orders.findAllWithItems().stream()
                .filter(o -> o.getStatus() != OrderStatus.PLACED) // Kanban shows PREPARING, OUT_FOR_DELIVERY, DELIVERED
                .map(OrdersController::toDto)
                .toList();
            return ResponseEntity.ok(res);
        }
        
        // Default: return current user's orders only
        List<OrderSummaryDto> res = orders.findByUserIdWithItemsOrderByCreatedAtDesc(uid).stream().map(OrdersController::toDto).toList();
        System.out.println("✅ Returning " + res.size() + " orders for user ID: " + uid);
        res.forEach(o -> System.out.println("   Order #" + o.id() + " - Total: ₹" + o.total() + " - Items: " + o.items().size()));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderSummaryDto> get(@PathVariable Long id){
        return orders.findByIdWithItems(id).map(OrdersController::toDto).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // Route endpoint removed (map/navigation feature not used)

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderSummaryDto> updateStatus(@PathVariable Long id, @RequestParam OrderStatus status){
        return orders.findById(id).map(o -> {
            o.setStatus(status);
            Instant now = Instant.now();
            switch (status){
                case PREPARING -> o.setPreparingAt(now);
                case OUT_FOR_DELIVERY -> {
                    o.setDispatchedAt(now);
                    if (o.getAssignedTo() != null) {
                        AgentOrderAssignment a = agentOrderRepository.findTopByOrderIdOrderByAssignedAtDesc(o.getId());
                        if (a != null) { a.setPickedUpAt(now); a.setStatus("OUT_FOR_DELIVERY"); agentOrderRepository.save(a); }
                    }
                }
                case DELIVERED -> {
                    o.setDeliveredAt(now);
                    if (o.getAssignedTo() != null) {
                        AgentOrderAssignment a = agentOrderRepository.findTopByOrderIdOrderByAssignedAtDesc(o.getId());
                        if (a != null) { a.setDeliveredAt(now); a.setStatus("DELIVERED"); agentOrderRepository.save(a); }
                    }
                }
                case CANCELLED -> o.setCancelledAt(now);
                default -> {}
            }
            orders.save(o);
            publishUpdate();
            return ResponseEntity.ok(toDto(o));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderSummaryDto> cancel(@PathVariable Long id){
        return orders.findById(id).map(o -> {
            if (o.getStatus() == OrderStatus.PLACED || o.getStatus() == OrderStatus.PREPARING){
                o.setStatus(OrderStatus.CANCELLED);
                o.setCancelledAt(Instant.now());
                orders.save(o);
                payments.findTopByOrder_IdOrderByCreatedAtDesc(o.getId()).ifPresent(p -> {
                    p.setStatus("REFUND_REQUESTED");
                    payments.save(p);
                });
                publishUpdate();
                return ResponseEntity.ok(toDto(o));
            }
            return ResponseEntity.status(409).<OrderSummaryDto>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // SSE stream for real-time updates
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(){
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        // push initial payload
        try { emitter.send(SseEmitter.event().name("snapshot").data("init")); } catch (Exception ignored) {}
        return emitter;
    }

    private void publishUpdate(){
        List<SseEmitter> dead = new ArrayList<>();
        emitters.forEach(em -> {
            try { em.send(SseEmitter.event().name("orders:update").data("changed")); }
            catch (Exception e){ dead.add(em); }
        });
        emitters.removeAll(dead);
    }

    // Agent: get assigned orders for current agent
    @PreAuthorize("hasRole('AGENT')")
    @GetMapping("/agent/my")
    public ResponseEntity<List<OrderSummaryDto>> myAssigned(){
        Long uid = currentUserId();
        if (uid == null) return ResponseEntity.status(401).build();
        List<OrderSummaryDto> res = orders.findByAssignedToWithItems(uid).stream()
                .map(OrdersController::toDto).toList();
        return ResponseEntity.ok(res);
    }

    // Assign order to current agent
    @PreAuthorize("hasRole('AGENT')")
    @PatchMapping("/{id}/assign/me")
    public ResponseEntity<OrderSummaryDto> assignMe(@PathVariable Long id){
        Long uid = currentUserId();
        if (uid == null) return ResponseEntity.status(401).build();
        return orders.findById(id).map(o -> {
            o.setAssignedTo(uid);
            orders.save(o);
            publishUpdate();
            return ResponseEntity.ok(toDto(o));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Agent: Accept order
    @PreAuthorize("hasRole('AGENT')")
    @PatchMapping("/{id}/accept")
    public ResponseEntity<OrderSummaryDto> acceptOrder(@PathVariable Long id){
        Long uid = currentUserId();
        if (uid == null) return ResponseEntity.status(401).build();
        
        return orders.findByIdWithItems(id).map(o -> {
            // Only allow accepting PLACED orders
            if (o.getStatus() != OrderStatus.PLACED) {
                return ResponseEntity.status(409).<OrderSummaryDto>build();
            }
            
            // Assign to current agent and mark as accepted
            o.setAssignedTo(uid);
            o.setStatus(OrderStatus.ACCEPTED);
            o.setPreparingAt(Instant.now()); // Mark when agent accepted
            orders.save(o);

            // Record assignment lifecycle row
            AgentOrderAssignment a = AgentOrderAssignment.builder()
                    .agentId(uid)
                    .orderId(o.getId())
                    .status("ACCEPTED")
                    .build();
            agentOrderRepository.save(a);
            publishUpdate();
            
            System.out.println("✅ Order " + id + " accepted by agent " + uid);
            return ResponseEntity.ok(toDto(o));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Agent: Reject order
    @PreAuthorize("hasRole('AGENT')")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<OrderSummaryDto> rejectOrder(@PathVariable Long id){
        Long uid = currentUserId();
        if (uid == null) return ResponseEntity.status(401).build();
        
        return orders.findByIdWithItems(id).map(o -> {
            // Only allow rejecting PLACED orders
            if (o.getStatus() != OrderStatus.PLACED) {
                return ResponseEntity.status(409).<OrderSummaryDto>build();
            }
            
            // Remove assignment and keep as PLACED for other agents
            o.setAssignedTo(null);
            orders.save(o);
            publishUpdate();
            
            System.out.println("❌ Order " + id + " rejected by agent " + uid);
            return ResponseEntity.ok(toDto(o));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Get unassigned orders for agents to accept/reject
    @PreAuthorize("hasRole('AGENT')")
    @GetMapping("/agent/available")
    public ResponseEntity<List<OrderSummaryDto>> getAvailableOrders(){
        List<OrderSummaryDto> res = orders.findAvailableOrdersWithItems().stream()
                .map(OrdersController::toDto)
                .toList();
        return ResponseEntity.ok(res);
    }
}
