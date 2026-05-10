package com.ezh.Inventory.payment.entity;

import com.ezh.Inventory.payment.entity.enums.RazorpayTransactionPurpose;
import com.ezh.Inventory.payment.entity.enums.RazorpayTransactionStatus;
import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "razorpay_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RazorpayTransaction extends CommonSerializable {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "razorpay_resource_id", nullable = false, unique = true, length = 100)
    private String razorpayResourceId;

    /** Razorpay payment ID (pay_xxx) — populated once payment is captured. */
    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;

    /** Original Razorpay method used to collect payment (PAYMENT_LINK, QR, CHECKOUT …). */
    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    @Column(name = "amount_in_paise", nullable = false)
    private Long amountInPaise;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RazorpayTransactionStatus status;

    /**
     * ID of the {@link Payment} record created once payment is confirmed.
     * Null until the payment is successfully recorded.
     */
    @Column(name = "payment_record_id")
    private Long paymentRecordId;


    /**
     * What this payment is for: INVOICE, MULTI_INVOICE, or ADVANCE.
     * Derived from the allocations list at order-creation time.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", length = 30)
    private RazorpayTransactionPurpose purpose;

    /**
     * Comma-separated invoice IDs this payment covers.
     * Example: "101" or "101,102,103". Null when purpose is ADVANCE.
     * Queryable without JSON parsing.
     */
    @Column(name = "invoice_ids", length = 500)
    private String invoiceIds;

    /**
     * JSON-serialised {@code List<PaymentAllocationDto>} captured at order
     * creation so that the webhook can allocate the payment to invoices
     * without requiring the frontend to re-submit them.
     */
    @Column(name = "allocations_json", columnDefinition = "TEXT")
    private String allocationsJson;

    @Column(name = "error_description", length = 500)
    private String errorDescription;

    @Column(name = "notes", length = 500)
    private String notes;
}
