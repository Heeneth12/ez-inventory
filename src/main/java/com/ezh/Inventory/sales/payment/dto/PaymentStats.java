package com.ezh.Inventory.sales.payment.dto;

import java.math.BigDecimal;

public interface PaymentStats {
    Long getTotalCount();            // Total number of payment records
    BigDecimal getTotalCollected();  // Sum of all 'amount' columns
    BigDecimal getTotalAllocated();  // Sum of 'allocated_amount' (Paid to bills)
    BigDecimal getTotalAdvance();    // Sum of 'unallocated_amount' (Excess/Advance)
    BigDecimal getPendingAmount();   // Sum of amount where status is PENDING (if applicable)
}
