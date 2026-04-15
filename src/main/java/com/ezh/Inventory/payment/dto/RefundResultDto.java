package com.ezh.Inventory.payment.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Returned from both refundFromWallet() and refundUnallocatedAmount().
 * Shows the total refunded and a breakdown of which source payments were drawn from.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResultDto {
    private String message;
    private BigDecimal totalRefunded;
    private List<PaymentRefundDto> refunds; // one entry per source payment touched
}
