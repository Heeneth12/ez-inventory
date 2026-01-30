package com.ezh.Inventory.purchase.grn.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrnItemDto {
    private Long poItemId; // Link to specific PO line
    private Long itemId;
    private String itemName;
    private Integer receivedQty;
    private Integer rejectedQty;
    private String batchNumber;
    private Long expiryDate;
}