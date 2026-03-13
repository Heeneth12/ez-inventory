package com.ezh.Inventory.sales.order.dto;

public interface SalesConversionCountProjection {
    Long getTotalSalesOrders();
    Long getConvertedToInvoice();
}
