package com.hungerexpress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartDTO {
    private List<CartItemDTO> items;
    private Integer totalItems;
    private Double subtotal;      // INR
    private Double deliveryFee;   // INR
    private Double tax;           // INR (GST)
    private Double total;         // INR
}
