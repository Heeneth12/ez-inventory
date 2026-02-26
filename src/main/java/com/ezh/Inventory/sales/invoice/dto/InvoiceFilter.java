package com.ezh.Inventory.sales.invoice.dto;

import com.ezh.Inventory.sales.invoice.entity.InvoicePaymentStatus;
import com.ezh.Inventory.sales.invoice.entity.InvoiceStatus;
import com.ezh.Inventory.utils.common.CommonFilter;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceFilter extends CommonFilter {
    private Long customerId;
    private Long salesOrderId;
    private List<InvoiceStatus> invStatuses;
    private List<InvoicePaymentStatus> paymentStatus;
}