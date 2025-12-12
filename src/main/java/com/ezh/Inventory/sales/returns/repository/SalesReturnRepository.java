package com.ezh.Inventory.sales.returns.repository;

import com.ezh.Inventory.sales.returns.entity.SalesReturn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalesReturnRepository extends JpaRepository<SalesReturn, Long> {
    Page<SalesReturn> findByTenantId(Long tenantId, Pageable pageable);
    Optional<SalesReturn> findByIdAndTenantId(Long id, Long tenantId);
}
