package com.ezh.Inventory.purchase.po.dto;

import com.ezh.Inventory.purchase.po.entity.PoStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseOrderDto {
    private Long id;
    private Long supplierId;
    private String supplierName;
    private Long warehouseId;
    private String orderNumber;
    private Long expectedDeliveryDate;
    private PoStatus status;
    private String notes;
    private BigDecimal totalAmount;
    private List<PurchaseOrderItemDto> items;
}