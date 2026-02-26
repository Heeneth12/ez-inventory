package com.ezh.Inventory.sales.invoice.dto;

import com.ezh.Inventory.sales.delivery.entity.ShipmentType;
import com.ezh.Inventory.sales.invoice.entity.InvoiceDeliveryStatus;
import com.ezh.Inventory.sales.invoice.entity.InvoicePaymentStatus;
import com.ezh.Inventory.sales.invoice.entity.InvoiceStatus;
import com.ezh.Inventory.sales.invoice.entity.InvoiceType;
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
    private String invoiceNumber;
    private Long salesOrderId;
    private String salesOrderNumber;
    private SalesOrderDto salesOrderDto;
    private Long customerId;
    private Long warehouseId;
    private String customerName;
    private UserMiniDto contactMini; // Optional, for detail view
    private Date invoiceDate;
    private InvoiceType invoiceType;
    private InvoiceStatus status;
    private InvoiceDeliveryStatus deliveryStatus;
    private InvoicePaymentStatus paymentStatus;

    //Financials (Header Level)
    private BigDecimal itemGrossTotal;
    private BigDecimal itemTotalDiscount;
    private BigDecimal itemTotalTax;

    private BigDecimal flatDiscountRate;   // INPUT from UI
    private BigDecimal flatDiscountAmount; // CALCULATED by backend

    private BigDecimal flatTaxRate;        // INPUT from UI
    private BigDecimal flatTaxAmount;      // CALCULATED by backend

    private BigDecimal grandTotal;
    private BigDecimal amountPaid;
    private BigDecimal balance;

    private String remarks;
    private List<InvoiceItemDto> items;
    //Delivery Details
    private ShipmentType deliveryType; // PICKUP, COURIER
    private Date scheduledDate;
    private String shippingAddress;
}
