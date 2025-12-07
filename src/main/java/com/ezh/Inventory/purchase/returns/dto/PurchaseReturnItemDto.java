package com.ezh.Inventory.purchase.returns.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseReturnItemDto {
    private Long itemId;
    private Integer returnQty;
    private String batchNumber;
    private BigDecimal refundPrice;
}