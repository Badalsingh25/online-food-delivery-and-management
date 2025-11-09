package com.hungerexpress.menu;

import com.hungerexpress.restaurant.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByRestaurant(Restaurant restaurant);
    List<MenuItem> findByCategoryIdOrderByIdAsc(Long categoryId);
    
    // Admin approval workflow queries
    List<MenuItem> findByApprovalStatusOrderBySubmittedAtDesc(MenuItem.ApprovalStatus status);
    List<MenuItem> findByRestaurantAndApprovalStatus(Restaurant restaurant, MenuItem.ApprovalStatus status);
    List<MenuItem> findByCategoryIdAndApprovalStatusOrderByIdAsc(Long categoryId, MenuItem.ApprovalStatus status);
    long countByApprovalStatus(MenuItem.ApprovalStatus status);
}
