package com.ezh.Inventory.stock.repository;

import com.ezh.Inventory.stock.entity.MovementType;
import com.ezh.Inventory.stock.entity.StockLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockLedgerRepository extends JpaRepository<StockLedger, Long> {

    Page<StockLedger> findByTenantId(Long tenantId, Pageable pageable);

    @Query("""
            SELECT s FROM StockLedger s
            WHERE s.tenantId = :tenantId
            AND (:id IS NULL OR s.id = :id)
            AND (:warehouseId IS NULL OR s.warehouseId = :warehouseId)
            AND (CAST(:transactionTypes AS text) IS NULL OR s.transactionType IN :transactionTypes)
            AND (CAST(:referenceType AS text) IS NULL OR s.referenceType IN :referenceType)
            AND (:searchQuery IS NULL OR 
                LOWER(s.uuid) LIKE LOWER(CAST(CONCAT('%', :searchQuery, '%') AS text)))
            AND (CAST(:fromDate AS timestamp) IS NULL OR s.createdAt >= :fromDate)
            AND (CAST(:toDate AS timestamp) IS NULL OR s.createdAt <= :toDate)
            """)
    Page<StockLedger> findAllStockLedger(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("warehouseId") Long warehouseId,
            @Param("transactionTypes") List<MovementType> transactionTypes,
            @Param("referenceType") List<String> referenceType,
            @Param("searchQuery") String searchQuery,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );
}