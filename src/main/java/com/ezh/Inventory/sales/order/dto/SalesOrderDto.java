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

    private Long id;              // only for update
    private Long tenantId;
    private Long warehouseId;
    private String orderNumber;   // SO-001, SO-2025-001
    private Date orderDate;
    private UserMiniDto contactMini;
    private Long customerId;      // Contact ID (Customer)
    private String customerName;
    private String paymentTerms;// "Net 30", "Advance", etc.
    private BigDecimal subTotal;
    private BigDecimal totalDiscount;
    private BigDecimal totalTax;
    private BigDecimal totalDiscountPer;
    private SalesOrderStatus status;
    private SalesOrderSource source;
    private BigDecimal grandTotal;
    private List<SalesOrderItemDto> items; // CHILD ITEMS
    private String remarks;
}
