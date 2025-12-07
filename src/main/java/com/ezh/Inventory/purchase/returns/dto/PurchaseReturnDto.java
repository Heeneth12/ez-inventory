package com.ezh.Inventory.purchase.returns.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseReturnDto {
    private Long supplierId;
    private Long warehouseId;
    private Long goodsReceiptId; // Optional link
    private String reason;
    private List<PurchaseReturnItemDto> items;
}
