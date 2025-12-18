package com.ezh.Inventory.approval.repository;


import com.ezh.Inventory.approval.entity.ApprovalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    List<ApprovalRequest> findByTenantIdAndStatus(Long tenantId, String status);
    Optional<ApprovalRequest> findByIdAndTenantId (Long approvalRequestId, Long tenantId);
    Page<ApprovalRequest> findByTenantId(Long tenantId, Pageable pageable);
}