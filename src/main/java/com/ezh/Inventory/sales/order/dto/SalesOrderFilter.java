package com.ezh.Inventory.sales.order.dto;

import com.ezh.Inventory.sales.order.entity.SalesOrderSource;
import com.ezh.Inventory.sales.order.entity.SalesOrderStatus;
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
public class SalesOrderFilter extends CommonFilter {
    private Long customerId;
    private String soNumber;
    private List<SalesOrderSource> soSource;
    private List<SalesOrderStatus> soStatuses;
}