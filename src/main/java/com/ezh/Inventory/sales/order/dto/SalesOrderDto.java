package com.ezh.Inventory.sales.order.dto;

import com.ezh.Inventory.sales.order.entity.SalesOrderSource;
import com.ezh.Inventory.sales.order.entity.SalesOrderStatus;
import com.ezh.Inventory.utils.common.dto.UserMiniDto;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrderDto {
    private Long id;
    private Long tenantId;
    private Long warehouseId;
    private String orderNumber;
    private Date orderDate;
    private UserMiniDto contactMini;
    private Long customerId;
    private String customerName;
    private String paymentTerms;
    private SalesOrderStatus status;
    private SalesOrderSource source;
    private String remarks;

    //Financial Fields (Aligned with Entity)
    private BigDecimal itemGrossTotal;
    private BigDecimal itemTotalDiscount;
    private BigDecimal itemTotalTax;

    private BigDecimal flatDiscountRate;   // User inputs 2%
    private BigDecimal flatDiscountAmount; // Backend calculates

    private BigDecimal flatTaxRate;        // User inputs 5%
    private BigDecimal flatTaxAmount;      // Backend calculates

    private BigDecimal grandTotal;

    private List<SalesOrderItemDto> items;
}
