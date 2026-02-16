package com.ezh.Inventory.purchase.returns.dto;

import com.ezh.Inventory.purchase.returns.entity.ReturnStatus;
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
public class PurchaseReturnFilter extends CommonFilter {
    private List<ReturnStatus> purchaseReturnStatuses;
    private Long vendorId;
}
