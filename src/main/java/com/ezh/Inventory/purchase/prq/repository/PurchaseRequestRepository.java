package com.ezh.Inventory.purchase.prq.repository;

import com.ezh.Inventory.purchase.prq.entity.PurchaseRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {

    Optional<PurchaseRequest> findByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT MAX(p.id) FROM PurchaseRequest p")
    Long findMaxId();

    @Query("""
            SELECT prq FROM PurchaseRequest prq
            WHERE prq.tenantId = :tenantId
              AND (:id IS NULL OR prq.id = :id)
              AND (:status IS NULL OR prq.status = :status)
              AND (
                    (CAST(:fromDate AS timestamp) IS NULL OR prq.createdAt >= :fromDate)
                    AND (CAST(:toDate AS timestamp) IS NULL OR prq.createdAt <= :toDate)
                  )
              AND (
                    CAST(:searchQuery AS string) IS NULL 
                    OR LOWER(prq.prqNumber) LIKE LOWER(CONCAT('%', CAST(:searchQuery AS string), '%'))
                    OR LOWER(prq.department) LIKE LOWER(CONCAT('%', CAST(:searchQuery AS string), '%'))
                  )
            """)
    Page<PurchaseRequest> findAllPurchaseRequests(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("status") String status, // Ensure this matches your Enum or String correctly
            @Param("searchQuery") String searchQuery,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );
}
