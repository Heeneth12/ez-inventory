package com.ezh.Inventory.sales.delivery.repository;

import com.ezh.Inventory.sales.delivery.entity.Delivery;
import com.ezh.Inventory.sales.delivery.entity.ShipmentStatus;
import com.ezh.Inventory.sales.delivery.entity.ShipmentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findByDeliveryNumber(String deliveryNumber);

    Optional<Delivery> findByIdAndTenantId(Long id, Long tenantId);

    List<Delivery> findByInvoiceId(Long invoiceId);

    List<Delivery> findByCustomerId(Long customerId);

    List<Delivery> findByStatus(ShipmentStatus status);

    @Query("SELECT d FROM Delivery d WHERE d.invoice.id = :invoiceId AND d.status = :status")
    List<Delivery> findByInvoiceIdAndStatus(@Param("invoiceId") Long invoiceId,
                                            @Param("status") ShipmentStatus status);

    @Query("""
            SELECT d FROM Delivery d
            WHERE d.tenantId = :tenantId
              AND (:deliveryId IS NULL OR d.id = :deliveryId)
              AND (:invoiceId IS NULL OR d.invoice.id = :invoiceId)
              AND (:customerId IS NULL OR d.customerId = :customerId)
              AND (:shipmentTypes IS NULL OR d.type IN :shipmentTypes)
              AND (:shipmentStatuses IS NULL OR d.status IN :shipmentStatuses)
              AND (
                    (CAST(:fromDate AS timestamp) IS NULL OR d.createdAt >= :fromDate)
                    AND (CAST(:toDate AS timestamp) IS NULL OR d.createdAt <= :toDate)
                  )
              AND (
                    CAST(:searchQuery AS string) IS NULL
                    OR LOWER(d.deliveryNumber) LIKE LOWER(CONCAT('%', CAST(:searchQuery AS string), '%'))
                  )
            """)
    Page<Delivery> findDeliveriesByFilter(
            @Param("tenantId") Long tenantId,
            @Param("deliveryId") Long deliveryId,
            @Param("invoiceId") Long invoiceId,
            @Param("customerId") Long customerId,
            @Param("shipmentTypes") List<ShipmentType> shipmentTypes,
            @Param("shipmentStatuses") List<ShipmentStatus> shipmentStatuses,
            @Param("searchQuery") String searchQuery,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );


    @Query("""
                SELECT d FROM Delivery d
                WHERE (:tenantId IS NULL OR d.tenantId = :tenantId)
                  AND (:id IS NULL OR d.id = :id)
                  AND (:deliveryNumber IS NULL OR d.deliveryNumber = :deliveryNumber)
                  AND (:invoiceId IS NULL OR d.invoice.id = :invoiceId)
                  AND (:customerId IS NULL OR d.customerId = :customerId)
                  AND (:types IS NULL OR d.type IN :types)
                  AND (:statuses IS NULL OR d.status IN :statuses)
                  AND (CAST(:fromDate AS timestamp) IS NULL OR d.createdAt >= :fromDate)
                  AND (CAST(:toDate AS timestamp) IS NULL OR d.createdAt <= :toDate)
            """)
    List<Delivery> searchDeliveries(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("deliveryNumber") String deliveryNumber,
            @Param("invoiceId") Long invoiceId,
            @Param("customerId") Long customerId,
            @Param("types") List<ShipmentType> types,
            @Param("statuses") List<ShipmentStatus> statuses,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );


    @Query("SELECT COUNT(d) FROM Delivery d WHERE d.tenantId = :tenantId AND d.status = :status")
    long countByStatus(@Param("tenantId") Long tenantId, @Param("status") ShipmentStatus status);

    @Query("SELECT COUNT(d) FROM Delivery d WHERE d.tenantId = :tenantId AND (d.status = 'SCHEDULED' OR d.status = 'SHIPPED')")
    long countPendingDeliveries(@Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(r) FROM Route r WHERE r.tenantId = :tenantId")
    long countTotalRoutes(@Param("tenantId") Long tenantId);
}

