package com.hungerexpress.service;

import com.hungerexpress.dto.CartDTO;
import com.hungerexpress.dto.CartItemDTO;
import com.hungerexpress.model.Cart;
import com.hungerexpress.menu.MenuItem;
import com.hungerexpress.menu.MenuItemRepository;
import com.hungerexpress.user.User;
import com.hungerexpress.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    
    private final CartRepository cartRepository;
    private final MenuItemRepository menuItemRepository;
    
    /**
     * Get all cart items for user
     */
    @Transactional(readOnly = true)
    public CartDTO getCart(User user) {
        List<Cart> cartItems = cartRepository.findByUserOrderByCreatedAtDesc(user);
        
        List<CartItemDTO> items = cartItems.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        Double total = cartRepository.calculateCartTotal(user);
        long itemCount = cartRepository.countByUser(user);
        
        return CartDTO.builder()
                .items(items)
                .totalItems((int) itemCount)
                .subtotal(total != null ? total : 0.0)
                .deliveryFee(50.0) // Fixed delivery fee in INR
                .tax(total != null ? total * 0.05 : 0.0) // 5% GST
                .total(total != null ? total + 50.0 + (total * 0.05) : 50.0)
                .build();
    }
    
    /**
     * Add item to cart
     */
    @Transactional
    public CartItemDTO addToCart(User user, Long menuItemId, Integer quantity) {
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));
        
        // Check if item already in cart
        Cart cart = cartRepository.findByUserAndMenuItemId(user, menuItemId)
                .orElse(null);
        
        if (cart != null) {
            // Update quantity
            cart.setQuantity(cart.getQuantity() + quantity);
            cart = cartRepository.save(cart);
            log.info("Updated cart item: {} quantity to {}", menuItem.getName(), cart.getQuantity());
        } else {
            // Create new cart item
            cart = new Cart();
            cart.setUser(user);
            cart.setMenuItem(menuItem);
            cart.setQuantity(quantity);
            cart.setPrice(menuItem.getPrice().doubleValue());
            cart = cartRepository.save(cart);
            log.info("Added {} to cart for user {}", menuItem.getName(), user.getEmail());
        }
        
        return convertToDTO(cart);
    }
    
    /**
     * Update cart item quantity
     */
    @Transactional
    public CartItemDTO updateCartItem(User user, Long cartItemId, Integer quantity) {
        Cart cart = cartRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        
        if (!cart.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        
        if (quantity <= 0) {
            cartRepository.delete(cart);
            log.info("Removed cart item: {}", cart.getMenuItem().getName());
            return null;
        }
        
        cart.setQuantity(quantity);
        cart = cartRepository.save(cart);
        log.info("Updated cart item: {} quantity to {}", cart.getMenuItem().getName(), quantity);
        
        return convertToDTO(cart);
    }
    
    /**
     * Remove item from cart
     */
    @Transactional
    public void removeFromCart(User user, Long cartItemId) {
        Cart cart = cartRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        
        if (!cart.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        
        cartRepository.delete(cart);
        log.info("Removed {} from cart", cart.getMenuItem().getName());
    }
    
    /**
     * Clear entire cart
     */
    @Transactional
    public void clearCart(User user) {
        cartRepository.deleteByUser(user);
        log.info("Cleared cart for user {}", user.getEmail());
    }
    
    /**
     * Get cart item count
     */
    @Transactional(readOnly = true)
    public long getCartItemCount(User user) {
        return cartRepository.countByUser(user);
    }
    
    /**
     * Convert Cart entity to DTO
     */
    private CartItemDTO convertToDTO(Cart cart) {
        MenuItem menuItem = cart.getMenuItem();
        return CartItemDTO.builder()
                .id(cart.getId())
                .menuItemId(menuItem.getId())
                .menuItemName(menuItem.getName())
                .restaurantId(menuItem.getRestaurant().getId())
                .restaurantName(menuItem.getRestaurant().getName())
                .price(cart.getPrice())
                .quantity(cart.getQuantity())
                .subtotal(cart.getSubtotal())
                .imageUrl(menuItem.getImageUrl())
                .build();
    }
}
