package com.ezh.Inventory.sales.invoice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceFilter {
    private Long id;
    private Long salesOrderId;
    private String status;
    private Long customerId;
    private Long warehouseId;
}
