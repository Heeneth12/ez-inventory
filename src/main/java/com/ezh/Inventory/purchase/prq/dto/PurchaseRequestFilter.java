package com.ezh.Inventory.purchase.prq.dto;

import com.ezh.Inventory.purchase.prq.entity.PrqStatus;
import com.ezh.Inventory.utils.common.CommonFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseRequestFilter extends CommonFilter {
    private Long vendorId;
    private List<PrqStatus> prqStatuses;
}
