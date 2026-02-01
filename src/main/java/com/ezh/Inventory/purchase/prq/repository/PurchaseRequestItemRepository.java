package com.ezh.Inventory.purchase.prq.repository;

import com.ezh.Inventory.purchase.prq.entity.PurchaseRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseRequestItemRepository extends JpaRepository<PurchaseRequestItem, Long> {
}
