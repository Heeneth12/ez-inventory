package com.ezh.Inventory.notifications.common.repository;

import com.ezh.Inventory.notifications.common.entity.Notification;
import com.ezh.Inventory.notifications.common.entity.NotificationType;
import com.ezh.Inventory.notifications.common.entity.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {


    Page<Notification> findByTargetScopeOrderByCreatedAtDesc(TargetType targetScope, Pageable pageable);


    List<Notification> findByTypeOrderByCreatedAtDesc(NotificationType type);


    List<Notification> findBySentByOrderByCreatedAtDesc(String sentBy);


    @Query("SELECT n FROM Notification n WHERE n.createdAt BETWEEN :from AND :to ORDER BY n.createdAt DESC")
    List<Notification> findBetween(@Param("from") Date from, @Param("to") Date to);


    /**
     * Fetch notification with its deliveries in one query.
     */
    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.deliveries WHERE n.id = :id")
    java.util.Optional<Notification> findByIdWithDeliveries(@Param("id") Long id);

    /**
     * Fetch multiple notifications with deliveries for an admin list view.
     */
    @Query("SELECT DISTINCT n FROM Notification n LEFT JOIN FETCH n.deliveries " +
            "WHERE n.targetScope = :scope ORDER BY n.createdAt DESC")
    List<Notification> findByScopeWithDeliveries(@Param("scope") TargetType scope);
}
