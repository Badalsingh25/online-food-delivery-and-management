package com.hungerexpress.cart;

public record CartItemDto(Long id, String name, double price, String imageUrl, int qty) {}
