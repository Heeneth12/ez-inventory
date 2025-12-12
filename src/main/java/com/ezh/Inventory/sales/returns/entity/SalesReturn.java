package com.ezh.Inventory.sales.returns.entity;

import com.ezh.Inventory.sales.invoice.entity.Invoice;
import com.ezh.Inventory.sales.payment.entity.Payment;
import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "sales_return")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReturn extends CommonSerializable {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "return_number", unique = true, nullable = false)
    private String returnNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "return_date", nullable = false)
    private Date returnDate;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "salesReturn", cascade = CascadeType.ALL)
    private List<SalesReturnItem> items = new ArrayList<>();

    // Link to the financial transaction we created
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_note_payment_id")
    private Payment creditNotePayment;
}