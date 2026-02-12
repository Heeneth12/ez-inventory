package com.ezh.Inventory.approval.repository;


import com.ezh.Inventory.approval.dto.ApprovalStatsDto;
import com.ezh.Inventory.approval.entity.ApprovalRequest;
import com.ezh.Inventory.approval.entity.ApprovalStatus;
import com.ezh.Inventory.approval.entity.ApprovalType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    List<ApprovalRequest> findByTenantIdAndApprovalStatus(Long tenantId, String status);

    Optional<ApprovalRequest> findByIdAndTenantId(Long approvalRequestId, Long tenantId);

    Page<ApprovalRequest> findByTenantId(Long tenantId, Pageable pageable);

    @Query("""
            SELECT a FROM ApprovalRequest a 
            WHERE a.tenantId = :tenantId
            AND (CAST(:approvalTypes AS text) IS NULL OR a.approvalType IN :approvalTypes)
            AND (CAST(:approvalStatuses AS text) IS NULL OR a.approvalStatus IN :approvalStatuses)
            AND (:searchQuery IS NULL OR 
                LOWER(a.referenceCode) LIKE LOWER(CAST(CONCAT('%', :searchQuery, '%') AS text)) OR 
                LOWER(a.description) LIKE LOWER(CAST(CONCAT('%', :searchQuery, '%') AS text)))
            AND (CAST(:fromDate AS timestamp) IS NULL OR a.createdAt >= :fromDate)
            AND (CAST(:toDate AS timestamp) IS NULL OR a.createdAt <= :toDate)
            """)
    Page<ApprovalRequest> searchApprovals(
            @Param("tenantId") Long tenantId,
            @Param("approvalTypes") List<ApprovalType> approvalTypes,
            @Param("approvalStatuses") List<ApprovalStatus> approvalStatuses,
            @Param("searchQuery") String searchQuery,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    @Query("""
                SELECT new com.ezh.Inventory.approval.dto.ApprovalStatsDto(
                    COUNT(a),
                    COALESCE(SUM(CASE WHEN a.approvalStatus = 'APPROVED' THEN 1 ELSE 0 END), 0),
                    COALESCE(SUM(CASE WHEN a.approvalStatus = 'PENDING' THEN 1 ELSE 0 END), 0),
                    COALESCE(SUM(CASE WHEN a.approvalStatus = 'REJECTED' THEN 1 ELSE 0 END), 0)
                )
                FROM ApprovalRequest a
                WHERE a.tenantId = :tenantId
            """)
    ApprovalStatsDto getApprovalStatistics(@Param("tenantId") Long tenantId);
}