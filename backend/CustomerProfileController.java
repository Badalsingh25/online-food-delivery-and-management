package com.hungerexpress.customer;

import com.hungerexpress.common.CurrentUser;
import com.hungerexpress.orders.OrderEntity;
import com.hungerexpress.orders.OrderRepository;
import com.hungerexpress.orders.OrderStatus;
import com.hungerexpress.user.User;
import com.hungerexpress.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer/profile")
@RequiredArgsConstructor
public class CustomerProfileController {

    private final UserRepository users;
    private final OrderRepository orders;
    private final PasswordEncoder passwordEncoder;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    // Get current user profile
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerProfileResponse> getProfile() {
        String email = CurrentUser.email();
        if (email == null) return ResponseEntity.status(401).build();

        User user = users.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        CustomerProfileResponse profile = CustomerProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .city(user.getCity())
                .state(user.getState())
                .postalCode(user.getPostalCode())
                .profilePictureUrl(user.getProfilePictureUrl())
                .profileCompleted(user.getProfileCompleted())
                .createdAt(user.getCreatedAt())
                .build();

        return ResponseEntity.ok(profile);
    }

    // Update profile
    @PutMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerProfileResponse> updateProfile(@RequestBody UpdateProfileRequest request) {
        String email = CurrentUser.email();
        if (email == null) return ResponseEntity.status(401).build();

        User user = users.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        // Update fields
        if (request.fullName() != null) user.setFullName(request.fullName());
        if (request.phone() != null) user.setPhone(request.phone());
        if (request.address() != null) user.setAddress(request.address());
        if (request.city() != null) user.setCity(request.city());
        if (request.state() != null) user.setState(request.state());
        if (request.postalCode() != null) user.setPostalCode(request.postalCode());
        if (request.profilePictureUrl() != null) user.setProfilePictureUrl(request.profilePictureUrl());
        
        user.setProfileCompleted(true);

        User saved = users.save(user);

        CustomerProfileResponse profile = CustomerProfileResponse.builder()
                .id(saved.getId())
                .email(saved.getEmail())
                .fullName(saved.getFullName())
                .phone(saved.getPhone())
                .address(saved.getAddress())
                .city(saved.getCity())
                .state(saved.getState())
                .postalCode(saved.getPostalCode())
                .profilePictureUrl(saved.getProfilePictureUrl())
                .profileCompleted(saved.getProfileCompleted())
                .createdAt(saved.getCreatedAt())
                .build();

        return ResponseEntity.ok(profile);
    }

    // Get order history
    @GetMapping("/orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<OrderHistoryDto>> getOrderHistory() {
        String email = CurrentUser.email();
        if (email == null) return ResponseEntity.status(401).build();

        User user = users.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        List<OrderEntity> userOrders = orders.findByUserIdOrderByCreatedAtDesc(user.getId());

        List<OrderHistoryDto> history = userOrders.stream()
                .map(order -> OrderHistoryDto.builder()
                        .orderId(order.getId())
                        .restaurantId(order.getRestaurantId())
                        .status(order.getStatus().name())
                        .subtotal(order.getSubtotal())
                        .discount(order.getDiscount())
                        .deliveryFee(order.getDeliveryFee())
                        .tax(order.getTax())
                        .total(order.getTotal())
                        .itemCount(order.getItems().size())
                        .createdAt(order.getCreatedAt())
                        .formattedDate(DATE_FORMAT.format(order.getCreatedAt()))
                        .placedAt(order.getPlacedAt())
                        .preparingAt(order.getPreparingAt())
                        .dispatchedAt(order.getDispatchedAt())
                        .deliveredAt(order.getDeliveredAt())
                        .cancelledAt(order.getCancelledAt())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }

    // Get order tracking details
    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderTrackingDto> getOrderTracking(@PathVariable Long orderId) {
        String email = CurrentUser.email();
        if (email == null) return ResponseEntity.status(401).build();

        User user = users.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        OrderEntity order = orders.findById(orderId).orElse(null);
        if (order == null || !order.getUserId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        OrderTrackingDto tracking = OrderTrackingDto.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .total(order.getTotal())
                .createdAt(order.getCreatedAt())
                .placedAt(order.getPlacedAt())
                .preparingAt(order.getPreparingAt())
                .dispatchedAt(order.getDispatchedAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .shipName(order.getShipName())
                .shipPhone(order.getShipPhone())
                .shipLine1(order.getShipLine1())
                .shipCity(order.getShipCity())
                .shipState(order.getShipState())
                .shipPostal(order.getShipPostal())
                .items(order.getItems().stream()
                        .map(item -> new OrderItemDto(
                                item.getId(),
                                item.getName(),
                                item.getPrice(),
                                item.getQty()
                        ))
                        .collect(Collectors.toList()))
                .build();

        return ResponseEntity.ok(tracking);
    }

    // DTOs
    record UpdateProfileRequest(
            String fullName,
            String phone,
            String address,
            String city,
            String state,
            String postalCode,
            String profilePictureUrl
    ) {}

    record OrderItemDto(Long id, String name, java.math.BigDecimal price, Integer qty) {}
}
