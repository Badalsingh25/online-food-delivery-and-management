package com.hungerexpress.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT o FROM OrderEntity o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<OrderEntity> findByIdWithItems(@Param("id") Long id);
    
    @Query("SELECT DISTINCT o FROM OrderEntity o LEFT JOIN FETCH o.items WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    List<OrderEntity> findByUserIdWithItemsOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    @Query("SELECT DISTINCT o FROM OrderEntity o LEFT JOIN FETCH o.items WHERE o.status = 'PLACED' AND o.assignedTo IS NULL ORDER BY o.createdAt ASC")
    List<OrderEntity> findAvailableOrdersWithItems();
    
    @Query("SELECT DISTINCT o FROM OrderEntity o LEFT JOIN FETCH o.items WHERE o.assignedTo = :agentId ORDER BY o.createdAt DESC")
    List<OrderEntity> findByAssignedToWithItems(@Param("agentId") Long agentId);
    
    @Query("SELECT DISTINCT o FROM OrderEntity o LEFT JOIN FETCH o.items ORDER BY o.createdAt DESC")
    List<OrderEntity> findAllWithItems();
}
