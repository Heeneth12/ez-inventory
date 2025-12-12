package com.ezh.Inventory.stock.dto;

import lombok.*;

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
    private List<BatchDetailDto> batches;
}