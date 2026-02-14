package com.ezh.Inventory.purchase.grn.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrnItemDto {
    private Long poItemId; // Link to specific PO line
    private BigDecimal poItemPrice; // For value calculations
    private Long itemId;
    private String itemName;
    private Integer receivedQty;
    private Integer rejectedQty;
    @Builder.Default
    private Integer returnedQty = 0;
    private String batchNumber;
    private Long expiryDate;
}