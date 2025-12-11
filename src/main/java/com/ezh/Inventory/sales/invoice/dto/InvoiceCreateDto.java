package com.ezh.Inventory.sales.invoice.dto;

import com.ezh.Inventory.sales.delivery.entity.ShipmentType;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceCreateDto {
    private Long salesOrderId;
    private Long customerId;
    private Long warehouseId;
    private String customerName;
    private Date invoiceDate;
    private List<InvoiceItemCreateDto> items;
    private BigDecimal discountAmount;
    private String remarks;

    private BigDecimal totalDiscount; // Flat discount on whole invoice
    private BigDecimal totalTax;
    //DELIVERY DETAILS ---
    private ShipmentType deliveryType; // PICKUP, COURIER
    private Date scheduledDate;   // For To-Do list
    private String shippingAddress;
}
