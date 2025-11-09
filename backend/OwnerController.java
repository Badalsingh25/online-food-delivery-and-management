package com.hungerexpress.owner;

import com.hungerexpress.common.CurrentUser;
import com.hungerexpress.orders.OrderEntity;
import com.hungerexpress.orders.OrderRepository;
import com.hungerexpress.orders.OrderStatus;
import com.hungerexpress.restaurant.Restaurant;
import com.hungerexpress.restaurant.RestaurantRepository;
import com.hungerexpress.user.User;
import com.hungerexpress.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class OwnerController {

    private final OrderRepository orders;
    private final UserRepository users;
    private final RestaurantRepository restaurants;
    
    // Store SSE emitters for live updates
    private final List<SseEmitter> emitters = new ArrayList<>();

    record TopItem(String name, int count) {}
    record DashboardSummary(
        int todayOrders,
        int pendingOrders,
        double revenueToday,
        int avgPrepTime,
        List<TopItem> topItems,
        List<Integer> hourlyTrends  // Array of 24 hours (0-23) with order counts
    ) {}

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Backend is working! User: " + CurrentUser.email());
    }

    record OrderResponse(
        long id,
        String status,
        double total,
        String createdAt,
        List<OrderItemInfo> items
    ) {}

    @GetMapping("/orders")
    @Transactional(readOnly = true)
    public ResponseEntity<List<OrderResponse>> getAllOrders(@RequestParam(required = false) String view) {
        try {
            System.out.println("[OwnerController] GET /api/owner/orders called with view=" + view);
            
            // Get current owner user
            String email = Optional.ofNullable(CurrentUser.email()).orElse(null);
            if (email == null) {
                return ResponseEntity.status(401).build();
            }
            
            // Get all orders (in real-world, filter by restaurant owned by this user)
            List<OrderEntity> allOrders = orders.findAll();
            System.out.println("[OwnerController] Found " + allOrders.size() + " total orders");
            
            List<OrderResponse> response = allOrders.stream()
                .map(order -> new OrderResponse(
                    order.getId(),
                    order.getStatus().name(),
                    order.getTotal().doubleValue(),
                    order.getCreatedAt().toString(),
                    order.getItems() != null ? order.getItems().stream()
                        .map(item -> new OrderItemInfo(item.getName(), item.getQty()))
                        .toList() : List.of()
                ))
                .toList();
            
            System.out.println("[OwnerController] Returning " + response.size() + " orders");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("[OwnerController] ERROR in getAllOrders: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping(value = "/orders/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamOrders(@RequestParam(required = false) String token) {
        System.out.println("[OwnerController] SSE connection request received");
        
        // Create SSE emitter with 30-minute timeout
        SseEmitter emitter = new SseEmitter(1800000L); // 30 minutes
        
        // Add to active emitters list
        synchronized (emitters) {
            emitters.add(emitter);
            System.out.println("[OwnerController] SSE client connected. Total clients: " + emitters.size());
        }
        
        // Remove emitter when completed or timed out
        emitter.onCompletion(() -> {
            synchronized (emitters) {
                emitters.remove(emitter);
                System.out.println("[OwnerController] SSE client disconnected. Total clients: " + emitters.size());
            }
        });
        
        emitter.onTimeout(() -> {
            synchronized (emitters) {
                emitters.remove(emitter);
                System.out.println("[OwnerController] SSE client timed out. Total clients: " + emitters.size());
            }
        });
        
        emitter.onError((ex) -> {
            synchronized (emitters) {
                emitters.remove(emitter);
                System.err.println("[OwnerController] SSE client error: " + ex.getMessage());
            }
        });
        
        // Send initial connection message
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("SSE connection established"));
        } catch (Exception e) {
            System.err.println("[OwnerController] Failed to send connection message: " + e.getMessage());
        }
        
        return emitter;
    }
    
    // Helper method to broadcast order updates to all connected clients
    public void broadcastOrderUpdate() {
        System.out.println("[OwnerController] Broadcasting order update to " + emitters.size() + " clients");
        
        synchronized (emitters) {
            List<SseEmitter> deadEmitters = new ArrayList<>();
            
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                        .name("orders:update")
                        .data("Order list updated"));
                } catch (Exception e) {
                    System.err.println("[OwnerController] Failed to send to client, marking as dead");
                    deadEmitters.add(emitter);
                }
            }
            
            // Remove dead emitters
            emitters.removeAll(deadEmitters);
        }
    }

    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public ResponseEntity<DashboardSummary> getDashboardSummary() {
        try {
            System.out.println("===========================================");
            System.out.println("[OwnerController] /api/owner/summary called");
            System.out.println("===========================================");
            
            // Get current owner user
            String email = Optional.ofNullable(CurrentUser.email()).orElse(null);
            System.out.println("[OwnerController] Current user email: " + email);
            if (email == null) {
                System.out.println("[OwnerController] No email found, returning 401");
                return ResponseEntity.status(401).build();
            }
            
            Optional<User> currentUser = users.findByEmail(email);
            if (currentUser.isEmpty()) {
                System.out.println("[OwnerController] User not found in database, returning 401");
                return ResponseEntity.status(401).build();
            }
            System.out.println("[OwnerController] User found: " + currentUser.get().getFullName());

        System.out.println("[OwnerController] Fetching all orders...");
        // Get all orders (in real-world, filter by restaurant owned by this user)
        List<OrderEntity> allOrders = orders.findAll();
        System.out.println("[OwnerController] Found " + allOrders.size() + " total orders");
        
        // Filter today's orders
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        System.out.println("[OwnerController] Today's date: " + today);
        List<OrderEntity> todayOrders = allOrders.stream()
            .filter(o -> {
                LocalDate orderDate = o.getCreatedAt()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
                return orderDate.equals(today);
            })
            .toList();
        System.out.println("[OwnerController] Found " + todayOrders.size() + " orders today");

        // Calculate metrics
        int todayOrdersCount = todayOrders.size();
        System.out.println("[OwnerController] Today orders count: " + todayOrdersCount);
        
        int pendingCount = allOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.PLACED || o.getStatus() == OrderStatus.PREPARING)
            .toList()
            .size();

        BigDecimal revenueToday = todayOrders.stream()
            .map(OrderEntity::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate average prep time (from PLACED to PREPARING)
        long totalPrepSeconds = todayOrders.stream()
            .filter(o -> o.getPlacedAt() != null && o.getPreparingAt() != null)
            .mapToLong(o -> o.getPreparingAt().getEpochSecond() - o.getPlacedAt().getEpochSecond())
            .sum();
        
        int ordersWithPrepTime = (int) todayOrders.stream()
            .filter(o -> o.getPlacedAt() != null && o.getPreparingAt() != null)
            .count();
        
        int avgPrepTimeMinutes = ordersWithPrepTime > 0 
            ? (int) (totalPrepSeconds / ordersWithPrepTime / 60) 
            : 0;

        // Get top items from today's orders
        Map<String, Integer> itemCounts = new HashMap<>();
        todayOrders.forEach(order -> {
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                order.getItems().forEach(item -> {
                    String name = item.getName();
                    itemCounts.put(name, itemCounts.getOrDefault(name, 0) + item.getQty());
                });
            }
        });

        List<TopItem> topItems = itemCounts.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(5)
            .map(e -> new TopItem(e.getKey(), e.getValue()))
            .toList();

        // Calculate hourly trends (0-23 hours)
        System.out.println("[OwnerController] Calculating hourly trends...");
        List<Integer> hourlyTrends = new ArrayList<>(Collections.nCopies(24, 0));
        todayOrders.forEach(order -> {
            int hour = order.getCreatedAt()
                .atZone(ZoneId.systemDefault())
                .getHour();
            hourlyTrends.set(hour, hourlyTrends.get(hour) + 1);
        });
        System.out.println("[OwnerController] Hourly trends calculated: " + hourlyTrends);

        System.out.println("[OwnerController] Building summary object...");
        DashboardSummary summary = new DashboardSummary(
            todayOrdersCount,
            pendingCount,
            revenueToday.doubleValue(),
            avgPrepTimeMinutes,
            topItems,
            hourlyTrends
        );

        System.out.println("[OwnerController] Returning summary: " + summary);
        return ResponseEntity.ok(summary);
        
        } catch (Exception e) {
            System.err.println("[OwnerController] ERROR in getDashboardSummary: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    record RestaurantOrdersResponse(
        long id,
        String status,
        double total,
        String createdAt,
        List<OrderItemInfo> items
    ) {}

    record OrderItemInfo(String name, int qty) {}

    @GetMapping("/restaurants/{rid}/orders")
    @Transactional(readOnly = true)
    public ResponseEntity<List<RestaurantOrdersResponse>> getRestaurantOrders(
        @PathVariable Long rid,
        @RequestParam(required = false) String status
    ) {
        // In real-world, verify owner owns this restaurant
        List<OrderEntity> restaurantOrders = orders.findAll().stream()
            .filter(o -> rid.equals(o.getRestaurantId()))
            .toList();

        // Filter by status if provided
        if (status != null && !status.isEmpty()) {
            try {
                OrderStatus statusEnum = OrderStatus.valueOf(status.toUpperCase());
                restaurantOrders = restaurantOrders.stream()
                    .filter(o -> o.getStatus() == statusEnum)
                    .toList();
            } catch (IllegalArgumentException ignored) {}
        }

        List<RestaurantOrdersResponse> response = restaurantOrders.stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .map(o -> new RestaurantOrdersResponse(
                o.getId(),
                o.getStatus().name(),
                o.getTotal().doubleValue(),
                o.getCreatedAt().toString(),
                o.getItems().stream()
                    .map(i -> new OrderItemInfo(i.getName(), i.getQty()))
                    .toList()
            ))
            .toList();

        return ResponseEntity.ok(response);
    }

    // Get all restaurants owned by this owner
    @GetMapping("/restaurants")
    @Transactional(readOnly = true)
    public ResponseEntity<List<RestaurantDto>> getOwnerRestaurants() {
        try {
            // Get current owner user
            String email = Optional.ofNullable(CurrentUser.email()).orElse(null);
            if (email == null) {
                return ResponseEntity.status(401).build();
            }
            
            Optional<User> currentUser = users.findByEmail(email);
            if (currentUser.isEmpty()) {
                return ResponseEntity.status(401).build();
            }
            
            Long ownerId = currentUser.get().getId();
            
            // Get all restaurants for this owner
            List<Restaurant> ownerRestaurants = restaurants.findByOwnerId(ownerId);
            
            // Map to DTO
            List<RestaurantDto> response = ownerRestaurants.stream()
                .map(r -> new RestaurantDto(
                    r.getId(),
                    r.getName(),
                    r.getDescription(),
                    r.getImageUrl(),
                    r.getCuisine().name(),
                    r.getRating(),
                    r.getDeliveryFee(),
                    r.getCity(),
                    r.getAddress(),
                    r.getPhone(),
                    r.getIsOnline(),
                    r.getApproved(),
                    r.getActive()
                ))
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("[OwnerController] ERROR in getOwnerRestaurants: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    record RestaurantDto(
        Long id,
        String name,
        String description,
        String imageUrl,
        String cuisine,
        Double rating,
        java.math.BigDecimal deliveryFee,
        String city,
        String address,
        String phone,
        Boolean isOnline,
        Boolean approved,
        Boolean active
    ) {}
}
