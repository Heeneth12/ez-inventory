package com.ezh.Inventory.sales.invoice.dto;

import com.ezh.Inventory.sales.invoice.entity.InvoiceStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceFilter {
    private Long id;
    private Long salesOrderId;
    private InvoiceStatus status;
    private Long customerId;
    private Long warehouseId;
}