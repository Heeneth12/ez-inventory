package com.ezh.Inventory.purchase.po.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseOrderItemDto {
    private Long itemId;
    private Integer orderedQty;
    private BigDecimal unitPrice;
}
