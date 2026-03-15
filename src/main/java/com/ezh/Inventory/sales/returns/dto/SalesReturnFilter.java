package com.ezh.Inventory.sales.returns.dto;

import com.ezh.Inventory.utils.common.CommonFilter;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReturnFilter extends CommonFilter {
    private Long customerId;
    private Long invoiceId;
    private String returnNumber;
}
