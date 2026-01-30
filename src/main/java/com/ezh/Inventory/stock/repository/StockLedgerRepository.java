package com.ezh.Inventory.stock.repository;

import com.ezh.Inventory.stock.entity.StockLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockLedgerRepository extends JpaRepository<StockLedger, Long>{

    Page<StockLedger> findByTenantId(Long tenantId, Pageable pageable);
}
