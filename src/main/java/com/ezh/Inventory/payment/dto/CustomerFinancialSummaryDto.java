package com.ezh.Inventory.payment.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerFinancialSummaryDto {
    private Long customerId;

    /**
     * Sum of all unpaid invoice balances
     */
    private BigDecimal totalOutstandingAmount;

    /**
     * Sum of CustomerAdvance.availableBalance — cash the customer deposited upfront
     */
    private BigDecimal advanceBalance;

    /**
     * Sum of CreditNote.availableBalance — value owed from returned goods
     */
    private BigDecimal creditNoteBalance;

    /**
     * Combined credit available = advanceBalance + creditNoteBalance
     */
    public BigDecimal getTotalCreditAvailable() {
        BigDecimal adv = advanceBalance != null ? advanceBalance : BigDecimal.ZERO;
        BigDecimal cn = creditNoteBalance != null ? creditNoteBalance : BigDecimal.ZERO;
        return adv.add(cn);
    }
}
