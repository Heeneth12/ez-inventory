package com.ezh.Inventory.purchase.returns.repository;

import com.ezh.Inventory.purchase.returns.entity.PurchaseReturn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, Long> {

    Optional<PurchaseReturn> findByIdAndTenantId(Long id, Long tenantId);

    Page<PurchaseReturn> findAllByTenantId(Long tenantId, Pageable pageable);


}
