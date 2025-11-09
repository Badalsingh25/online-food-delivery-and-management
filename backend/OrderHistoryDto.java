package com.hungerexpress.customer;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class OrderHistoryDto {
    private Long orderId;
    private Long restaurantId;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal deliveryFee;
    private BigDecimal tax;
    private BigDecimal total;
    private Integer itemCount;
    private Instant createdAt;
    private String formattedDate;
    
    // Status timestamps for tracking
    private Instant placedAt;
    private Instant preparingAt;
    private Instant dispatchedAt;
    private Instant deliveredAt;
    private Instant cancelledAt;
}
