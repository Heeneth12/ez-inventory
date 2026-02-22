package com.ezh.Inventory.purchase.prq.dto;

import com.ezh.Inventory.utils.common.CommonFilter;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseRequestFilter extends CommonFilter {
    private Long vendorId;
}
