package com.ezh.Inventory.sales.order.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrderItemDto {
    private Long id;
    private Long itemId; // selected item ID
    private String itemName;
    private Integer quantity;
    private Integer orderedQty;
    private BigDecimal tax;
    private Integer invoicedQty = 0;
    private BigDecimal unitPrice;     // price before discount
    private BigDecimal discount;
    private BigDecimal discountPercent;   // optional per item
    private BigDecimal lineTotal;
}
