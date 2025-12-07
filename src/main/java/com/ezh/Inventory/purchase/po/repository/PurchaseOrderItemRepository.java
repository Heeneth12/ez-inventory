package com.ezh.Inventory.purchase.po.repository;

import com.ezh.Inventory.purchase.po.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    List<PurchaseOrderItem> findByPurchaseOrderId(Long purchaseOrderId);
    void deleteByPurchaseOrderId(Long purchaseOrderId);

    @Query("SELECT COUNT(i) FROM PurchaseOrderItem i WHERE i.purchaseOrderId = :poId AND i.receivedQty > 0")
    long countReceivedItemsForPo(@Param("poId") Long poId);
}
