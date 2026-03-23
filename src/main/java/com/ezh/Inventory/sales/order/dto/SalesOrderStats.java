package com.ezh.Inventory.sales.order.dto;

import java.math.BigDecimal;

public interface SalesOrderStats {
    BigDecimal getTotalValue();
    BigDecimal getConvertedSalesValue();
    Long getTotalSalesOrders();
    Long getConvertedToInvoiceCount();
    Long getConfirmedCount();
    Long getPendingApprovalCount();
    Long getCancelledCount();
}
