package com.ezh.Inventory.contacts.entiry;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "address")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address extends CommonSerializable {

    private String addressLine1;
    private String addressLine2;
    private String route;
    private String area;
    private String city;
    private String state;
    private String country;
    private String pinCode;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "type", nullable = false, columnDefinition = "address_type")
    private AddressType type; // BILLING, SHIPPING, OFFICE, etc.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;
}