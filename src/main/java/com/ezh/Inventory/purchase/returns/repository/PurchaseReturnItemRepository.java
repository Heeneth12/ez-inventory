package com.ezh.Inventory.purchase.returns.repository;

import com.ezh.Inventory.purchase.returns.entity.PurchaseReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseReturnItemRepository extends JpaRepository<PurchaseReturnItem, Long> {

    List<PurchaseReturnItem> findByPurchaseReturnId(Long returnId);

}
