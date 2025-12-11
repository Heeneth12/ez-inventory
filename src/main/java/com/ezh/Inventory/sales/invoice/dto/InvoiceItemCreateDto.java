package com.ezh.Inventory.sales.invoice.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceItemCreateDto {
    private Long id;
    private Long soItemId; // Link to specific SO Line
    private Long itemId;
    private Integer quantity;
    private String batchNumber; // Optional (Specific Batch)
    private BigDecimal unitPrice; // Optional override
    private BigDecimal discountAmount; // Per item discount
    private BigDecimal taxAmount;      // Per item tax
}
