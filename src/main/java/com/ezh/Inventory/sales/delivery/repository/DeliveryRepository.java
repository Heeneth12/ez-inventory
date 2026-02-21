package com.ezh.Inventory.sales.delivery.repository;

import com.ezh.Inventory.sales.delivery.entity.Delivery;
import com.ezh.Inventory.sales.delivery.entity.ShipmentStatus;
import com.ezh.Inventory.sales.delivery.entity.ShipmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
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
                WHERE (:tenantId IS NULL OR d.tenantId = :tenantId)
                  AND (:id IS NULL OR d.id = :id)
                  AND (:deliveryNumber IS NULL OR d.deliveryNumber LIKE %:deliveryNumber%)
                  AND (:invoiceId IS NULL OR d.invoice.id = :invoiceId)
                  AND (:customerId IS NULL OR d.customerId = :customerId)
                  AND (:type IS NULL OR d.type = :type)
                  AND (:status IS NULL OR d.status = :status)
                  AND (:scheduledDate IS NULL OR d.scheduledDate >= :scheduledDate)
                  AND (:shippedDate IS NULL OR d.shippedDate >= :shippedDate)
                  AND (:deliveredDate IS NULL OR d.deliveredDate >= :deliveredDate)
            """)
    List<Delivery> searchDeliveries(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("deliveryNumber") String deliveryNumber,
            @Param("invoiceId") Long invoiceId,
            @Param("customerId") Long customerId,
            @Param("type") ShipmentType type,
            @Param("status") ShipmentStatus status,
            @Param("scheduledDate") Date scheduledDate,
            @Param("shippedDate") Date shippedDate,
            @Param("deliveredDate") Date deliveredDate
    );


    @Query("SELECT COUNT(d) FROM Delivery d WHERE d.tenantId = :tenantId AND d.status = :status")
    long countByStatus(@Param("tenantId") Long tenantId, @Param("status") ShipmentStatus status);

    @Query("SELECT COUNT(d) FROM Delivery d WHERE d.tenantId = :tenantId AND (d.status = 'SCHEDULED' OR d.status = 'SHIPPED')")
    long countPendingDeliveries(@Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(r) FROM Route r WHERE r.tenantId = :tenantId")
    long countTotalRoutes(@Param("tenantId") Long tenantId);
}

