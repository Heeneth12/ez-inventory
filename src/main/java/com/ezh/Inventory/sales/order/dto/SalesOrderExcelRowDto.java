package com.ezh.Inventory.sales.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderExcelRowDto {
    private Long id;
    private String orderNumber;
    private Date orderDate;
    private String status;
    private String source;
    private Long customerId;
    private Long warehouseId;
    private BigDecimal itemGrossTotal;
    private BigDecimal itemTotalDiscount;
    private BigDecimal itemTotalTax;
    private BigDecimal grandTotal;
    private String remarks;
}
