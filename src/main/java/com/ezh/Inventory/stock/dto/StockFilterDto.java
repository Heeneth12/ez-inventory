package com.ezh.Inventory.stock.dto;

import com.ezh.Inventory.stock.entity.AdjustmentStatus;
import com.ezh.Inventory.utils.common.CommonFilter;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class StockFilterDto extends CommonFilter {
    private Long itemId;
    private List<AdjustmentStatus> stockAdjustmentStatuses;
    private String stockAdjustmentNumber;
}