package com.ezh.Inventory.sales.order.dto;

import java.sql.Date;

public interface SalesConversionDateProjection {
    Date getReportDate();
    Long getTotalSalesOrders();
    Long getConvertedToInvoice();
}
