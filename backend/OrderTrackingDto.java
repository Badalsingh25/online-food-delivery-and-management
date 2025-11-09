package com.hungerexpress.customer;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class OrderTrackingDto {
    private Long orderId;
    private String status;
    private BigDecimal total;
    private Instant createdAt;
    
    // Tracking timestamps
    private Instant placedAt;
    private Instant preparingAt;
    private Instant dispatchedAt;
    private Instant deliveredAt;
    private Instant cancelledAt;
    
    // Delivery address
    private String shipName;
    private String shipPhone;
    private String shipLine1;
    private String shipCity;
    private String shipState;
    private String shipPostal;
    
    // Order items
    private List<CustomerProfileController.OrderItemDto> items;
}
