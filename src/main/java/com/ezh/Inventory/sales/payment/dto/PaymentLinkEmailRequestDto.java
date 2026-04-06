package com.ezh.Inventory.sales.payment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLinkEmailRequestDto {

    @NotBlank(message = "Payment link ID is required")
    private String paymentLinkId;

    /** The short URL to embed in the email body. */
    @NotBlank(message = "Payment link URL is required")
    private String paymentLinkUrl;

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    /** Recipient email address supplied by the operator. */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String email;

    /** Optional — shown in the email subject when present. */
    private Long invoiceId;
}
