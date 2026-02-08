package com.ezh.Inventory.purchase.prq.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseRequestItemDto {
    private Long Id;
    private Long itemId;
    private String itemName;
    private Integer requestedQty;
    private BigDecimal estimatedUnitPrice;
    private BigDecimal lineTotal;
}
