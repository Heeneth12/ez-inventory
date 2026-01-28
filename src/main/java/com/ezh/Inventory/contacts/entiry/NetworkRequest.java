package com.ezh.Inventory.contacts.entiry;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "network_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkRequest extends CommonSerializable {

    @Column(name = "sender_tenant_id", nullable = false)
    private Long senderTenantId;

    @Column(name = "receiver_tenant_id", nullable = false)
    private Long receiverTenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private NetworkStatus status;

    private String message;

    private String senderBusinessName;
}
