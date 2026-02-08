package com.ezh.Inventory.contacts.entiry;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contact")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contact extends CommonSerializable {

    @Column(name = "contact_code", nullable = false)
    private String contactCode;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    private String name;
    private String email;
    private String phone;
    private String gstNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", nullable = false, length = 50)
    private ContactType contactType;

    @Column(name = "credit_days", nullable = true)
    private Integer creditDays;

    private Boolean active;

    @Column(name = "connected_tenant_id")
    private Long connectedTenantId;

    /**
     * Links to the accepted network request that established this connection.
     * FetchType.LAZY is recommended to avoid loading this heavy object unless needed.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "network_request_id")
    private NetworkRequest networkRequest;

    @OneToMany(mappedBy = "contact", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Address> addresses = new ArrayList<>();

}

