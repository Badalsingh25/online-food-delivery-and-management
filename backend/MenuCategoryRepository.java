package com.hungerexpress.menu;

import com.hungerexpress.restaurant.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {
    List<MenuCategory> findByRestaurantOrderByPositionAsc(Restaurant restaurant);
}
