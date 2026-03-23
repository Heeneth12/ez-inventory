package com.ezh.Inventory.sales.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesConversionReportDto {
    private Long totalSalesOrders;
    private Long convertedToInvoice;
    private Long pendingConversion;
    private BigDecimal conversionRate;
    private BigDecimal totalSalesValue;
    private BigDecimal convertedSalesValue;
    private Long pendingApprovalCount;
    private Long cancelledRejectedCount;
    private LocalDate reportDate;
}

