package com.hungerexpress.repository;

import com.hungerexpress.model.Cart;
import com.hungerexpress.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    
    /**
     * Find all cart items for a user
     */
    List<Cart> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * Find specific cart item by user and menu item
     */
    @Query("SELECT c FROM Cart c WHERE c.user = :user AND c.menuItem.id = :menuItemId")
    Optional<Cart> findByUserAndMenuItemId(@Param("user") User user, @Param("menuItemId") Long menuItemId);
    
    /**
     * Count items in user's cart
     */
    long countByUser(User user);
    
    /**
     * Calculate total cart value for user
     */
    @Query("SELECT SUM(c.price * c.quantity) FROM Cart c WHERE c.user = :user")
    Double calculateCartTotal(@Param("user") User user);
    
    /**
     * Delete all cart items for user
     */
    void deleteByUser(User user);
    
    /**
     * Delete all cart items for user by restaurant
     */
    @Query("DELETE FROM Cart c WHERE c.user = :user AND c.menuItem.restaurant.id = :restaurantId")
    void deleteByUserAndRestaurantId(@Param("user") User user, @Param("restaurantId") Long restaurantId);
}
