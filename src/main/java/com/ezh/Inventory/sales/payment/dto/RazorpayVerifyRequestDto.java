package com.ezh.Inventory.sales.payment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayVerifyRequestDto {

    @NotBlank(message = "Razorpay order ID is required")
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay payment ID is required")
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay signature is required")
    private String razorpaySignature;

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Amount must be at least ₹1")
    private BigDecimal amount;

    /** Optional invoice allocations; absent = advance/wallet top-up */
    @Valid
    private List<PaymentAllocationDto> allocations;

    private String remarks;

    /**
     * Only required when the original order was a QR payment —
     * used to close the QR code after successful capture.
     */
    private String qrCodeId;

    /**
     * Only required when the original order was a Payment Link —
     * used to cancel / close the link after successful capture.
     */
    private String paymentLinkId;
}
