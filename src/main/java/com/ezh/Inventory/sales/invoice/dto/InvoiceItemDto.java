package com.ezh.Inventory.sales.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemDto {
    private Long id;
    private Long soItemId; // Link to SO Line
    private Long itemId;
    private String itemName;
    private String sku;

    //Quantities & Stock
    private Integer quantity; // How much is being invoiced now
    private Integer returnedQuantity;
    private String batchNumber; // The specific batch deducted

    //Financials (Line Level)
    private BigDecimal unitPrice;

    private BigDecimal discountRate;   // INPUT from UI
    private BigDecimal discountAmount;

    private BigDecimal taxRate;        // INPUT from UI
    private BigDecimal taxAmount;

    private BigDecimal lineTotal;
}
