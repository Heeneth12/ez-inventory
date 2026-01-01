package com.ezh.Inventory.sales.invoice.entity;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItem extends CommonSerializable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    //LINK TO SALES ORDER (For Partial Invoicing)
    @Column(name = "so_item_id")
    private Long soItemId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "sku")
    private String sku;

    // --- STOCK CONTROL ---
    // Critical for Profit Calculation.
    // Tells the StockService exactly which cost price to use from the Ledger.
    @Column(name = "batch_number")
    private String batchNumber;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Builder.Default
    @Column(name = "discount_amount")
    private BigDecimal discountAmount = BigDecimal.ZERO; // optional per item

    @Column(name = "returned_quantity")
    private Integer returnedQuantity = 0;

    @Builder.Default
    @Column(name = "tax_amount")
    private BigDecimal taxAmount = BigDecimal.ZERO; // tax per item

    @Column(name = "line_total", nullable = false)
    private BigDecimal lineTotal; // qty × price − discount + tax
}