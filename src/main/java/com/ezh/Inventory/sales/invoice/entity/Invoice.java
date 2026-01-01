package com.ezh.Inventory.sales.invoice.entity;

import com.ezh.Inventory.contacts.entiry.Contact;
import com.ezh.Inventory.sales.order.entity.SalesOrder;
import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", length = 20, columnDefinition = "invoice_status")
    private InvoiceStatus status;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "delivery_status", length = 20, columnDefinition = "invoice_delivery_status")
    private InvoiceDeliveryStatus deliveryStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "payment_status", length = 20, columnDefinition = "invoice_payment_status")
    private InvoicePaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "invoice_type", length = 30, columnDefinition = "invoice_type")
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
