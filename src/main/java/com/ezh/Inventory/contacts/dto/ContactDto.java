package com.ezh.Inventory.contacts.dto;

import com.ezh.Inventory.contacts.entiry.ContactType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContactDto {
    private Long id;
    private Long tenantId;
    private String contactCode;
    private String name;
    private String email;
    private String phone;
    private String gstNumber;
    private Integer creditDays;
    private ContactType type;// VENDOR / CUSTOMER / BOTH
    private Boolean active;
    private Long connectedTenantId;
    private List<AddressDto> addresses;
}
