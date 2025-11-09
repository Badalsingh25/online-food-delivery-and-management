package com.hungerexpress.reviews;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    List<ReviewEntity> findByMenuItemIdOrderByCreatedAtDesc(Long menuItemId);
    List<ReviewEntity> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);
}
