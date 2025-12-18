package com.ezh.Inventory.sales.payment.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerFinancialSummaryDto {
    private Long customerId;
    private BigDecimal totalOutstandingAmount;
    private BigDecimal walletBalance;
}