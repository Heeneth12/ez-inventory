package com.ezh.Inventory.purchase.returns.repository;

import com.ezh.Inventory.purchase.returns.entity.PurchaseReturn;
import com.ezh.Inventory.purchase.returns.entity.ReturnStatus;
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
public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, Long> {

    Optional<PurchaseReturn> findByIdAndTenantId(Long id, Long tenantId);

    Page<PurchaseReturn> findAllByTenantId(Long tenantId, Pageable pageable);

    @Query("""
            SELECT p FROM PurchaseReturn p 
            WHERE p.tenantId = :tenantId
            AND (:vendorId IS NULL OR p.vendorId = :vendorId)
            AND (CAST(:statuses AS text) IS NULL OR p.prStatus IN :statuses)
            AND (:searchQuery IS NULL OR 
                LOWER(p.prNumber) LIKE LOWER(CAST(CONCAT('%', :searchQuery, '%') AS text)) OR 
                LOWER(p.reason) LIKE LOWER(CAST(CONCAT('%', :searchQuery, '%') AS text)))
            AND (CAST(:fromDate AS timestamp) IS NULL OR p.createdAt >= :fromDate)
            AND (CAST(:toDate AS timestamp) IS NULL OR p.createdAt <= :toDate)
            """)
    Page<PurchaseReturn> findAllPR(
            @Param("tenantId") Long tenantId,
            @Param("vendorId") Long vendorId,
            @Param("statuses") List<ReturnStatus> statuses,
            @Param("searchQuery") String searchQuery,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

}
