package com.ezh.Inventory.purchase.po.dto;

import com.ezh.Inventory.purchase.po.entity.PoStatus;
import com.ezh.Inventory.utils.common.CommonFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseOrderFilter extends CommonFilter {
    private Long vendorId;
    private List<PoStatus> poStatuses;
}