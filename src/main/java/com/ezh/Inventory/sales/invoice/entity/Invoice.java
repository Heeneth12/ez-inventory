package com.ezh.Inventory.sales.invoice.entity;

import com.ezh.Inventory.sales.order.entity.SalesOrder;
import com.ezh.Inventory.utils.AbstractFinancialHeader;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
public class Invoice extends AbstractFinancialHeader {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 40)
    private String invoiceNumber;

    @Column(name = "invoice_date", nullable = false)
    private Date invoiceDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private InvoiceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", length = 50, nullable = false)
    private InvoiceDeliveryStatus deliveryStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 50, nullable = false)
    private InvoicePaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", length = 50, nullable = false)
    private InvoiceType invoiceType;

    //INVOICE SPECIFIC FINANCIALS
    @Column(name = "amount_paid", nullable = false)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();

    // INHERITED FROM AbstractFinancialHeader:
    // - itemGrossTotal
    // - itemTotalDiscount, itemTotalTax
    // - flatDiscountRate, flatDiscountAmount
    // - flatTaxRate, flatTaxAmount
    // - grandTotal
}
