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
    private Long itemId;
    private String itemName;
    private Integer orderedQty;
    private Integer invoicedQty;

    //Financial Fields (Aligned with Entity)
    private BigDecimal unitPrice;
    private BigDecimal discountRate;   // User inputs 10%
    private BigDecimal discountAmount; // Backend calculates
    private BigDecimal taxRate;        // User inputs 18%
    private BigDecimal taxAmount;      // Backend calculates
    private BigDecimal lineTotal;
}