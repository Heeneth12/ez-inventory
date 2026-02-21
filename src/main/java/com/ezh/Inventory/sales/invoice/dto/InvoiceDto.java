package com.ezh.Inventory.sales.invoice.dto;

import com.ezh.Inventory.sales.invoice.entity.InvoiceDeliveryStatus;
import com.ezh.Inventory.sales.invoice.entity.InvoicePaymentStatus;
import com.ezh.Inventory.sales.invoice.entity.InvoiceStatus;
import com.ezh.Inventory.sales.order.dto.SalesOrderDto;
import com.ezh.Inventory.utils.common.dto.UserMiniDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDto {
    private Long id;
    private String invoiceNumber; // INV-2025-0001
    private Long salesOrderId;
    private SalesOrderDto salesOrderDto;
    private UserMiniDto contactMini;
    private Integer progressStep; //UI
    private Long customerId;
    private InvoiceStatus status;
    private InvoiceDeliveryStatus deliveryStatus;
    private InvoicePaymentStatus paymentStatus;
    private Date invoiceDate;
    private List<InvoiceItemDto> items;
    private BigDecimal subTotal; // qty × price (sum of all line totals before tax)
    private BigDecimal discountAmount; // optional (invoice level)
    private BigDecimal taxAmount; // total tax
    private BigDecimal totalDiscount = BigDecimal.ZERO;
    private BigDecimal totalTax = BigDecimal.ZERO;
    private BigDecimal grandTotal; // subTotal - discount + tax
    private BigDecimal amountPaid; // how much customer paid
    private BigDecimal balance; // outstanding amount
    private String remarks;
}
