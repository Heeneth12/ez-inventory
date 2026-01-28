package com.ezh.Inventory.contacts.repository;

import com.ezh.Inventory.contacts.entiry.NetworkRequest;
import com.ezh.Inventory.contacts.entiry.NetworkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NetworkRequestRepository extends JpaRepository<NetworkRequest, Long> {

    Boolean existsBySenderTenantIdAndReceiverTenantId(Long senderTenantId, Long receiverTenantId);

    @Query("SELECT n FROM NetworkRequest n WHERE " +
            "(n.senderTenantId = :tenantId OR n.receiverTenantId = :tenantId) " +
            "AND n.status = :status")
    List<NetworkRequest> findAllConnections(Long tenantId, NetworkStatus status);

    @Query("SELECT n FROM NetworkRequest n WHERE n.receiverTenantId = :tenantId AND n.status = 'PENDING'")
    List<NetworkRequest> findIncomingRequests(Long tenantId);

    List<NetworkRequest> findByReceiverTenantIdAndStatus(Long receiverId, NetworkStatus status);

    // Check if any relationship (Pending or Connected) already exists
    @Query("SELECT COUNT(n) > 0 FROM NetworkRequest n WHERE " +
            "((n.senderTenantId = :t1 AND n.receiverTenantId = :t2) OR " +
            "(n.senderTenantId = :t2 AND n.receiverTenantId = :t1))")
    boolean existsConnection(@Param("t1") Long t1, @Param("t2") Long t2);
}
