package com.ezh.Inventory.stock.dto;

import com.ezh.Inventory.stock.entity.MovementType;
import com.ezh.Inventory.stock.entity.ReferenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockLedgerDto {
    private Long id;
    private Long itemId;
    private String itemName;
    private Long tenantId;
    private Long warehouseId;
    private MovementType transactionType;
    private Integer quantity;
    private ReferenceType referenceType;
    private Long referenceId;
    private Integer beforeQty;
    private Integer afterQty;
    private Date createdAt;
}
