package com.ezh.Inventory.stock.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ItemStockSearchDto {
    private Long itemId;
    private String name;
    private String code;
    private String sku;
    private String hsnSacCode;
    private String imageUrl;
    private BigDecimal sellingPrice;
    private BigDecimal discountRate;
    private BigDecimal taxRate;
    private List<BatchDetailDto> batches;
}