package com.ezh.Inventory.contacts.dto;

import com.ezh.Inventory.contacts.entiry.Contact;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContactMiniDto {
    private Long id;
    private String contactCode;
    private String name;

    public ContactMiniDto(Contact contact){
        this.id = contact.getId();
        this.contactCode = contact.getContactCode();
        this.name = contact.getName();
    }
}
