package com.ezh.Inventory.sales.order.dto;

import java.math.BigDecimal;

public interface SalesOrderStats {
    BigDecimal getTotalValue();
    Long getConfirmedCount();
    Long getPendingApprovalCount();
    Long getCancelledCount();
}
