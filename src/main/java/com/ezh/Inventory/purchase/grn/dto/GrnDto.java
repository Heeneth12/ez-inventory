package com.ezh.Inventory.purchase.grn.dto;

import com.ezh.Inventory.purchase.grn.entity.GrnStatus;
import com.ezh.Inventory.utils.common.dto.UserMiniDto;

import lombok.*;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GrnDto {
    private Long id;
    private Long vendorId;
    private String grnNumber;
    private Long purchaseOrderId;
    private String purchaseOrderNumber;
    private String supplierInvoiceNo; // Optional
    private GrnStatus status;
    private Date createdAt;
    private UserMiniDto vendorDetails;
    private List<GrnItemDto> items;
}
