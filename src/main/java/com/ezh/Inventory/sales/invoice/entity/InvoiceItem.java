package com.ezh.Inventory.sales.invoice.entity;

import com.ezh.Inventory.utils.AbstractFinancialLine;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "invoice_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItem extends AbstractFinancialLine {

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

    //STOCK CONTROL
    @Column(name = "batch_number")
    private String batchNumber;

    @Column(name = "returned_quantity")
    private Integer returnedQuantity = 0;

    // INHERITED FROM AbstractFinancialLine:
    // - quantity
    // - unitPrice
    // - discountRate, discountAmount
    // - taxRate, taxAmount
    // - lineTotal
}