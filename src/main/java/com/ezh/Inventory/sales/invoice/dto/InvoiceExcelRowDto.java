package com.ezh.Inventory.sales.invoice.dto;

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
public class InvoiceExcelRowDto {
    private Long id;
    private String invoiceNumber;
    private Date invoiceDate;
    private Long salesOrderId;
    private String salesOrderNumber;
    private String status;
    private String paymentStatus;
    private Long customerId;
    private Long warehouseId;
    private BigDecimal itemGrossTotal;
    private BigDecimal itemTotalDiscount;
    private BigDecimal itemTotalTax;
    private BigDecimal grandTotal;
    private BigDecimal amountPaid;
    private BigDecimal balance;
    private String remarks;
}
