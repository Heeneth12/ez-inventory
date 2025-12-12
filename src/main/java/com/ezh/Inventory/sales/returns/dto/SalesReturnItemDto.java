package com.ezh.Inventory.sales.returns.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReturnItemDto {
    private Long id;
    private Long itemId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private String reason;
}
