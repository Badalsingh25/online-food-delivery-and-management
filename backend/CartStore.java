package com.hungerexpress.cart;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CartStore {
    private final Map<String, List<CartItemDto>> carts = new ConcurrentHashMap<>();
    public List<CartItemDto> get(String email){ return carts.computeIfAbsent(email, k -> new ArrayList<>()); }
    public void clear(String email){ get(email).clear(); }
}
