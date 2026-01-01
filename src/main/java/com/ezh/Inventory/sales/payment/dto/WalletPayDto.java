package com.ezh.Inventory.sales.payment.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WalletPayDto {
    private Long customerId;
    private Long paymentId;      // The credit note / advance payment ID
    private Long invoiceId;      // The invoice to pay
    private BigDecimal amount;   // Amount to move from wallet to invoice
}
