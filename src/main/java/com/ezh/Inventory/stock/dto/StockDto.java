package com.ezh.Inventory.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockDto {
    private Long id;
    private Long itemId;
    private String itemName;
    private Long tenantId;
    private Long warehouseId;
    private Integer openingQty = 0;
    private Integer inQty = 0;
    private Integer outQty = 0;
    private Integer closingQty = 0;
    private BigDecimal averageCost = BigDecimal.ZERO;
    private BigDecimal stockValue = BigDecimal.ZERO;
}
