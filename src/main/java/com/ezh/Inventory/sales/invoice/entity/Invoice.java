package com.ezh.Inventory.sales.invoice.entity;

import com.ezh.Inventory.contacts.entiry.Contact;
import com.ezh.Inventory.sales.order.entity.SalesOrder;
import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "invoice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends CommonSerializable {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId; // Where stock is deducted from

    @Column(name = "invoice_number", nullable = false, unique = true, length = 40)
    private String invoiceNumber; // INV-2025-0001

    @Column(name = "invoice_date", nullable = false)
    private Date invoiceDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder; // Which Sales Order this invoice belongs to

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Contact customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvoiceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 20)
    private InvoiceDeliveryStatus deliveryStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private InvoicePaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 30)
    private InvoiceType invoiceType;

    @Column(name = "sub_total", nullable = false)
    private BigDecimal subTotal; // qty × price (sum of all line totals before tax)

    @Builder.Default
    @Column(name = "total_discount")
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_tax")
    private BigDecimal totalTax = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false)
    private BigDecimal grandTotal; // subTotal - discount + tax

    @Column(name = "amount_paid", nullable = false)
    private BigDecimal amountPaid; // how much customer paid

    @Column(name = "balance", nullable = false)
    private BigDecimal balance; // outstanding amount

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Builder.Default
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();
}
