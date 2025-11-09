package com.hungerexpress.orders;

import java.util.List;

public record OrderSummaryDto(Long id, String status, double total, Long createdAt, List<OrderItemDto> items) {}
