package com.ezh.Inventory.purchase.grn.dto;

import com.ezh.Inventory.purchase.grn.entity.GrnStatus;
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
public class GrnFilter extends CommonFilter {
    private Long vendorId;
    private List<GrnStatus> grnStatuses;
}
