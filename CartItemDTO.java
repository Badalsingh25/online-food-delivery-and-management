package com.hungerexpress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    private Long id;
    private Long menuItemId;
    private String menuItemName;
    private Long restaurantId;
    private String restaurantName;
    private Double price;      // INR - price per item
    private Integer quantity;
    private Double subtotal;   // INR - price * quantity
    private String imageUrl;
}
