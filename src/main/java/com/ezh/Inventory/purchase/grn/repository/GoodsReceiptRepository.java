package com.ezh.Inventory.purchase.grn.repository;

import com.ezh.Inventory.purchase.grn.entity.GoodsReceipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {

    Optional<GoodsReceipt> findByIdAndTenantId(Long id, Long tenantId);
    List<GoodsReceipt> findByPurchaseOrderIdAndTenantId(Long purchaseOrderId, Long tenantId);
    Page<GoodsReceipt> findByTenantId(Long tenantId,  Pageable pageable);

    /**
     * Optimized query that fetches GoodsReceipt with ALL related entities in a single query.
     * Uses Sets instead of Lists to avoid MultipleBagFetchException.
     * Fetches: PurchaseOrder, GoodsReceiptItems, Items, PurchaseOrderItems, PurchaseReturns, and PurchaseReturnItems.
     */
    @Query("""
        SELECT DISTINCT gr FROM GoodsReceipt gr
        LEFT JOIN FETCH gr.purchaseOrder po
        LEFT JOIN FETCH gr.items gri
        LEFT JOIN FETCH gri.item
        LEFT JOIN FETCH gri.poItem
        LEFT JOIN FETCH gr.purchaseReturns pr
        LEFT JOIN FETCH pr.purchaseReturnItems
        WHERE gr.id = :grnId AND gr.tenantId = :tenantId
        """)
    Optional<GoodsReceipt> findByIdWithAllRelations(@Param("grnId") Long grnId, @Param("tenantId") Long tenantId);

    /**
     * Batch query to fetch multiple GRNs with all their relations using IN clause.
     * Much more efficient than calling findByIdWithAllRelations in a loop.
     */
    @Query("""
        SELECT DISTINCT gr FROM GoodsReceipt gr
        LEFT JOIN FETCH gr.purchaseOrder po
        LEFT JOIN FETCH gr.items gri
        LEFT JOIN FETCH gri.item
        LEFT JOIN FETCH gri.poItem
        LEFT JOIN FETCH gr.purchaseReturns pr
        LEFT JOIN FETCH pr.purchaseReturnItems
        WHERE gr.id IN :grnIds AND gr.tenantId = :tenantId
        """)
    List<GoodsReceipt> findAllByIdWithRelations(@Param("grnIds") List<Long> grnIds, @Param("tenantId") Long tenantId);
}
