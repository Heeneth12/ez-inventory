package com.ezh.Inventory.sales.order.repository;

import java.time.LocalDate;

public interface SalesConversionDateProjection {
    LocalDate getReportDate();
    Long getTotalSalesOrders();
    Long getConvertedToInvoice();
}
