package com.ezh.Inventory.purchase.po.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseOrderItemDto {
    private Long id;
    private Long itemId;
    private String itemName;
    private Integer orderedQty;
    private BigDecimal unitPrice;
    private BigDecimal discount;
    private BigDecimal tax;
}
