package com.ezh.Inventory.purchase.grn.repository;

import com.ezh.Inventory.purchase.grn.entity.GoodsReceipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {

    Optional<GoodsReceipt> findByIdAndTenantId(Long id, Long tenantId);
    List<GoodsReceipt> findByPurchaseOrderIdAndTenantId(Long purchaseOrderId, Long tenantId);
    Page<GoodsReceipt> findByTenantId(Long tenantId,  Pageable pageable);
}
