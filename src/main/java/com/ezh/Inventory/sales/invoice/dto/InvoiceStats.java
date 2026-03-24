package com.ezh.Inventory.sales.invoice.dto;

import java.math.BigDecimal;

public interface InvoiceStats {
    BigDecimal getTotalInvoiceValue();
    BigDecimal getCollectedAmount();
    BigDecimal getUncollectedAmount();
    Long getPendingCount();
    Long getPendingDeliveryCount();
}
