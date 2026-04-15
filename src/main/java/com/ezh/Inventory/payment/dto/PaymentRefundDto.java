package com.ezh.Inventory.payment.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRefundDto {
    private Long id;
    private String refundNumber;
    private Long customerId;
    private Long sourcePaymentId;
    private String sourcePaymentNumber; // e.g. ADV-2025-0001 — easy for UI to display
    private BigDecimal refundAmount;
    private Date refundDate;
    private String refundReferenceNumber;
    private String remarks;
}
