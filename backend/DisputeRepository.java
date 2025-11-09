package com.hungerexpress.dispute;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DisputeRepository extends JpaRepository<DisputeEntity, Long> {
    
    List<DisputeEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<DisputeEntity> findByOrderId(Long orderId);
    
    List<DisputeEntity> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);
    
    List<DisputeEntity> findByStatusOrderByCreatedAtDesc(DisputeEntity.DisputeStatus status);
    
    @Query("SELECT d FROM DisputeEntity d WHERE d.status IN :statuses ORDER BY d.createdAt DESC")
    List<DisputeEntity> findByStatusIn(@Param("statuses") List<DisputeEntity.DisputeStatus> statuses);
    
    @Query("SELECT COUNT(d) FROM DisputeEntity d WHERE d.status = :status")
    Long countByStatus(@Param("status") DisputeEntity.DisputeStatus status);
}
