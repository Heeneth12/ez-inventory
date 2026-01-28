package com.ezh.Inventory.contacts.dto;

import com.ezh.Inventory.contacts.entiry.NetworkStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NetworkRequestDto {
    private Long id;
    private Long senderTenantId;
    private Long receiverTenantId;
    private NetworkStatus status;
    private String message;
    private String senderBusinessName;
    private Date createdAt;
}
