package com.ezh.Inventory.purchase.po.repository;

import com.ezh.Inventory.purchase.po.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    Optional<PurchaseOrder> findByIdAndTenantId(Long id, Long tenantId);
    Page<PurchaseOrder> findAllByTenantId(Long tenantId, Pageable pageable);
}
