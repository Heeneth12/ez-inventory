package com.ezh.Inventory.payment.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAllocationDto {
    private Long invoiceId;
    private BigDecimal amountToPay;
}