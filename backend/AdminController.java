package com.hungerexpress.admin;

import com.hungerexpress.user.User;
import com.hungerexpress.user.UserRepository;
import com.hungerexpress.restaurant.Restaurant;
import com.hungerexpress.restaurant.RestaurantRepository;
import com.hungerexpress.orders.OrderEntity;
import com.hungerexpress.orders.OrderRepository;
import com.hungerexpress.orders.OrderStatus;
import com.hungerexpress.common.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository users;
    private final RestaurantRepository restaurants;
    private final OrderRepository orders;
    private final AdminAuditLogRepository audits;

    @GetMapping("/users")
    public List<User> listUsers(@RequestParam(required = false) String q){
        List<User> all = users.findAll();
        if (q == null || q.isBlank()) return all;
        final String s = q.toLowerCase();
        return all.stream().filter(u ->
                (u.getEmail()!=null && u.getEmail().toLowerCase().contains(s))
             || (u.getFullName()!=null && u.getFullName().toLowerCase().contains(s))
        ).toList();
    }

    @PatchMapping("/users/{id}/enabled")
    public ResponseEntity<User> setEnabled(@PathVariable Long id, @RequestParam boolean enabled){
        return users.findById(id).map(u -> {
            u.setEnabled(enabled);
            users.save(u);
            String actor = Optional.ofNullable(CurrentUser.email()).orElse("system");
            audits.save(AdminAuditLog.builder().actorEmail(actor).action("USER_ENABLED")
                    .target("user:"+u.getId()).details("enabled="+enabled).build());
            return ResponseEntity.ok(u);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<User> setRole(@PathVariable Long id, @RequestParam String role){
        return users.findById(id).map(u -> {
            u.setRole(role.toUpperCase());
            users.save(u);
            String actor = Optional.ofNullable(CurrentUser.email()).orElse("system");
            audits.save(AdminAuditLog.builder().actorEmail(actor).action("USER_ROLE_CHANGE")
                    .target("user:"+u.getId()).details("role="+role.toUpperCase()).build());
            return ResponseEntity.ok(u);
        }).orElse(ResponseEntity.notFound().build());
    }

    // Restaurants moderation
    @GetMapping("/restaurants")
    public List<Restaurant> listRestaurants(@RequestParam(required = false) Boolean approved,
                                            @RequestParam(required = false) Boolean active,
                                            @RequestParam(required = false) String q){
        List<Restaurant> all = restaurants.findAll();
        return all.stream().filter(r ->
                (approved == null || approved.equals(r.getApproved()))
             && (active == null || active.equals(r.getActive()))
             && (q == null || q.isBlank() || (r.getName()!=null && r.getName().toLowerCase().contains(q.toLowerCase())))
        ).toList();
    }

    @PatchMapping("/restaurants/{id}/approve")
    public ResponseEntity<Restaurant> approveRestaurant(@PathVariable Long id, @RequestParam boolean approved){
        return restaurants.findById(id).map(r -> {
            r.setApproved(approved);
            restaurants.save(r);
            String actor = Optional.ofNullable(CurrentUser.email()).orElse("system");
            audits.save(AdminAuditLog.builder().actorEmail(actor).action("RESTAURANT_APPROVE")
                    .target("restaurant:"+r.getId()).details("approved="+approved).build());
            return ResponseEntity.ok(r);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/restaurants/{id}/active")
    public ResponseEntity<Restaurant> setRestaurantActive(@PathVariable Long id, @RequestParam boolean active){
        return restaurants.findById(id).map(r -> {
            r.setActive(active);
            restaurants.save(r);
            String actor = Optional.ofNullable(CurrentUser.email()).orElse("system");
            audits.save(AdminAuditLog.builder().actorEmail(actor).action("RESTAURANT_ACTIVE")
                    .target("restaurant:"+r.getId()).details("active="+active).build());
            return ResponseEntity.ok(r);
        }).orElse(ResponseEntity.notFound().build());
    }

    // Orders oversight
    @GetMapping("/orders")
    public List<OrderEntity> listOrders(@RequestParam(required = false) OrderStatus status,
                                        @RequestParam(required = false) Long userId){
        List<OrderEntity> all = orders.findAll();
        return all.stream().filter(o ->
                (status == null || status.equals(o.getStatus()))
             && (userId == null || userId.equals(o.getUserId()))
        ).toList();
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<OrderEntity> overrideOrderStatus(@PathVariable Long id, @RequestParam OrderStatus status){
        return orders.findById(id).map(o -> {
            o.setStatus(status);
            orders.save(o);
            String actor = Optional.ofNullable(CurrentUser.email()).orElse("system");
            audits.save(AdminAuditLog.builder().actorEmail(actor).action("ORDER_STATUS_OVERRIDE")
                    .target("order:"+o.getId()).details("status="+status.name()).build());
            return ResponseEntity.ok(o);
        }).orElse(ResponseEntity.notFound().build());
    }
}
