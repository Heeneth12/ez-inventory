package com.ezh.Inventory.sales.payment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderRequestDto {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Amount must be at least ₹1")
    private BigDecimal amount;

    /**
     * Payment channel: UPI, QR, NET_BANKING
     */
    @NotNull(message = "Payment method is required")
    private RazorpayPaymentMethod paymentMethod;

    /**
     * Required for UPI method — customer's UPI VPA (e.g. user@upi)
     */
    @Pattern(regexp = "^[\\w.+-]+@[\\w]+$", message = "Invalid UPI ID format")
    private String upiId;

    /**
     * Required for NET_BANKING — Razorpay bank code (e.g. SBIN, HDFC)
     */
    @Size(min = 2, max = 10, message = "Invalid bank code")
    private String bankCode;

    /**
     * Optional invoice allocations; if empty, amount goes to wallet as advance
     */
    @Valid
    private List<PaymentAllocationDto> allocations;

    private String notes;
}
