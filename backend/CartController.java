package com.hungerexpress.cart;

import com.hungerexpress.common.CurrentUser;
import com.hungerexpress.dto.CartDTO;
import com.hungerexpress.dto.CartItemDTO;
import com.hungerexpress.service.CartService;
import com.hungerexpress.user.User;
import com.hungerexpress.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Database-backed Cart Controller
 * Replaces in-memory cart with persistent database storage
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    /**
     * Get user's cart with all items
     */
    @GetMapping
    public ResponseEntity<CartDTO> getCart() {
        String email = CurrentUser.email();
        System.out.println("[CartController] GET /api/cart - email: " + email);
        if (email == null) {
            // Return empty cart for guest users
            System.out.println("[CartController] Guest user, returning empty cart");
            CartDTO emptyCart = new CartDTO(java.util.List.of(), 0, 0.0, 0.0, 0.0, 0.0);
            return ResponseEntity.ok(emptyCart);
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            System.out.println("[CartController] User not found for email: " + email);
            return ResponseEntity.status(404).build();
        }

        CartDTO cart = cartService.getCart(user);
        System.out.println("[CartController] Returning cart with " + cart.getItems().size() + " items");
        return ResponseEntity.ok(cart);
    }

    /**
     * Add item to cart (or update quantity if already exists)
     */
    @PostMapping("/add")
    public ResponseEntity<CartItemDTO> addToCart(@RequestBody AddToCartRequest request) {
        String email = CurrentUser.email();
        System.out.println("[CartController] POST /api/cart/add - email: " + email + ", itemId: " + request.menuItemId());
        if (email == null) {
            System.out.println("[CartController] Guest user trying to add to cart - returning 401 to use localStorage");
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            System.out.println("[CartController] User not found for email: " + email);
            return ResponseEntity.status(404).build();
        }

        CartItemDTO cartItem = cartService.addToCart(user, request.menuItemId(), request.quantity());
        System.out.println("[CartController] Added item to cart successfully");
        return ResponseEntity.ok(cartItem);
    }

    /**
     * Update cart item quantity
     */
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartItemDTO> updateCartItem(
            @PathVariable Long cartItemId,
            @RequestBody UpdateCartItemRequest request) {
        String email = CurrentUser.email();
        System.out.println("[CartController] PUT /api/cart/items/" + cartItemId + " - email: " + email + ", quantity: " + request.quantity());
        if (email == null) {
            System.out.println("[CartController] No user email, returning 401");
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            System.out.println("[CartController] User not found for email: " + email);
            return ResponseEntity.status(404).build();
        }

        CartItemDTO cartItem = cartService.updateCartItem(user, cartItemId, request.quantity());
        if (cartItem == null) {
            // Item was removed because quantity was 0
            System.out.println("[CartController] Item removed (quantity = 0)");
            return ResponseEntity.noContent().build();
        }
        
        System.out.println("[CartController] Cart item updated successfully");
        return ResponseEntity.ok(cartItem);
    }

    /**
     * Remove item from cart
     */
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Void> removeFromCart(@PathVariable Long cartItemId) {
        String email = CurrentUser.email();
        System.out.println("[CartController] DELETE /api/cart/items/" + cartItemId + " - email: " + email);
        if (email == null) {
            System.out.println("[CartController] No user email, returning 401");
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            System.out.println("[CartController] User not found for email: " + email);
            return ResponseEntity.status(404).build();
        }

        cartService.removeFromCart(user, cartItemId);
        System.out.println("[CartController] Cart item removed successfully");
        return ResponseEntity.noContent().build();
    }

    /**
     * Clear entire cart
     */
    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        String email = CurrentUser.email();
        System.out.println("[CartController] DELETE /api/cart - email: " + email);
        if (email == null) {
            System.out.println("[CartController] No user email, returning 401");
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            System.out.println("[CartController] User not found for email: " + email);
            return ResponseEntity.status(404).build();
        }

        cartService.clearCart(user);
        System.out.println("[CartController] Cart cleared successfully");
        return ResponseEntity.noContent().build();
    }

    /**
     * Get cart item count (for badge)
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getCartCount() {
        String email = CurrentUser.email();
        if (email == null) {
            return ResponseEntity.ok(0L);
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(0L);
        }

        long count = cartService.getCartItemCount(user);
        return ResponseEntity.ok(count);
    }

    // DTOs
    public record AddToCartRequest(Long menuItemId, Integer quantity) {}
    public record UpdateCartItemRequest(Integer quantity) {}
}
