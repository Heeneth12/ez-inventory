package com.ezh.Inventory.sales.returns.repository;

import com.ezh.Inventory.sales.returns.entity.SalesReturn;
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
public interface SalesReturnRepository extends JpaRepository<SalesReturn, Long> {
    Page<SalesReturn> findByTenantId(Long tenantId, Pageable pageable);

    @Query("""
            SELECT sr FROM SalesReturn sr
            WHERE sr.tenantId = :tenantId
              AND (:id IS NULL OR sr.id = :id)
              AND (:customerId IS NULL OR sr.invoice.customerId = :customerId)
              AND (:invoiceId IS NULL OR sr.invoice.id = :invoiceId)
              AND (:warehouseId IS NULL OR sr.invoice.warehouseId = :warehouseId)
              AND (:statuses IS NULL OR sr.invoice.status IN :statuses)
              AND (
                    (CAST(:fromDate AS timestamp) IS NULL OR sr.returnDate >= :fromDate)
                    AND (CAST(:toDate AS timestamp) IS NULL OR sr.returnDate <= :toDate)
                  )
              AND (
                    CAST(:searchQuery AS string) IS NULL
                    OR LOWER(sr.returnNumber) LIKE LOWER(CONCAT('%', CAST(:searchQuery AS string), '%'))
                    OR LOWER(sr.invoice.invoiceNumber) LIKE LOWER(CONCAT('%', CAST(:searchQuery AS string), '%'))
                  )
            """)
    Page<SalesReturn> getAllSalesReturn(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("customerId") Long customerId,
            @Param("invoiceId") Long invoiceId,
            @Param("warehouseId") Long warehouseId,
            @Param("statuses") List<String> statuses,
            @Param("searchQuery") String searchQuery,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    Optional<SalesReturn> findByIdAndTenantId(Long id, Long tenantId);
}
