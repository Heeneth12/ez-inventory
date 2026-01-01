package com.ezh.Inventory.contacts.entiry;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contact")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contact extends CommonSerializable{
    private String contactCode;
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    private String name;
    private String email;
    private String phone;
    private String gstNumber;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "type", nullable = false, columnDefinition = "contact_type")
    private ContactType type;
    @Column(name = "credit_days", nullable = true)
    private Integer creditDays;
    private Boolean active;
    @OneToMany(mappedBy = "contact", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Address> addresses = new ArrayList<>();

}

