package com.hungerexpress.orders;

public record OrderItemDto(Long id, String name, double price, int qty) {}
