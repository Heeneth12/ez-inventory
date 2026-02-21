package com.ezh.Inventory.sales.delivery.repository;

import com.ezh.Inventory.sales.delivery.entity.Route;
import com.ezh.Inventory.sales.delivery.entity.RouteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    Optional<Route> findByIdAndTenantId(Long id, Long tenantId);
    Page<Route> findByTenantId(Long tenantId, Pageable pageable);

    @Query("SELECT r FROM Route r WHERE r.tenantId = :tenantId AND r.status = :status")
    List<Route> findByStatus(@Param("tenantId") Long tenantId, @Param("status") RouteStatus status);

    @Query("SELECT r FROM Route r " +
            "LEFT JOIN FETCH r.deliveries d " +
            "WHERE r.id = :routeId AND r.tenantId = :tenantId")
    Optional<Route> findByIdWithDeliveries(@Param("routeId") Long routeId, @Param("tenantId") Long tenantId);
}
