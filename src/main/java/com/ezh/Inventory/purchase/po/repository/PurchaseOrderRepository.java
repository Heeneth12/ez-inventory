package com.ezh.Inventory.purchase.po.repository;

import com.ezh.Inventory.purchase.po.entity.PoStatus;
import com.ezh.Inventory.purchase.po.entity.PurchaseOrder;
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
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    Optional<PurchaseOrder> findByIdAndTenantId(Long id, Long tenantId);
    Page<PurchaseOrder> findAllByTenantId(Long tenantId, Pageable pageable);

    @Query("""
    SELECT po FROM PurchaseOrder po
    WHERE po.tenantId = :tenantId
      AND (:id IS NULL OR po.id = :id)
      AND (:statuses IS NULL OR po.poStatus IN :statuses)
      AND (:vendorId IS NULL OR po.vendorId = :vendorId)
      AND (:warehouseId IS NULL OR po.warehouseId = :warehouseId)
      AND (
            (CAST(:fromDate AS timestamp) IS NULL OR po.createdAt >= :fromDate)
            AND (CAST(:toDate AS timestamp) IS NULL OR po.createdAt <= :toDate)
          )
      AND (
            CAST(:searchQuery AS string) IS NULL
            OR LOWER(po.orderNumber) LIKE LOWER(CONCAT('%', CAST(:searchQuery AS string), '%'))
          )
    """)
    Page<PurchaseOrder> findAllPurchaseOrders(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("statuses") List<PoStatus> statuses,
            @Param("vendorId") Long vendorId,
            @Param("warehouseId") Long warehouseId,
            @Param("searchQuery") String searchQuery,
            @Param("fromDate") LocalDateTime fromDate, // Changed to LocalDateTime
            @Param("toDate") LocalDateTime toDate,     // Changed to LocalDateTime
            Pageable pageable
    );
}
