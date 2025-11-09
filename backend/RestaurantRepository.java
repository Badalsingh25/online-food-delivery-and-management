package com.hungerexpress.restaurant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    @Query("""
        select r from Restaurant r
        where r.approved = true and r.active = true
          and (:cuisine is null or r.cuisine = :cuisine)
          and (:minRating is null or r.rating >= :minRating)
          and (
                :search is null or lower(r.name) like lower(concat('%', :search, '%'))
                or lower(r.city) like lower(concat('%', :search, '%'))
                or lower(r.description) like lower(concat('%', :search, '%'))
          )
        order by r.rating desc
        """)
    List<Restaurant> search(
            @Param("search") String search,
            @Param("cuisine") Cuisine cuisine,
            @Param("minRating") Double minRating
    );
    
    // Find restaurants by owner ID
    List<Restaurant> findByOwnerId(Long ownerId);
    
    // Find active restaurants by owner
    List<Restaurant> findByOwnerIdAndActiveTrue(Long ownerId);
}
