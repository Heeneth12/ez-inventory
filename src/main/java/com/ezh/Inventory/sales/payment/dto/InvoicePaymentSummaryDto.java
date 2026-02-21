package com.ezh.Inventory.sales.payment.dto;

import com.ezh.Inventory.sales.invoice.entity.InvoiceStatus;
import com.ezh.Inventory.utils.common.dto.UserMiniDto;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoicePaymentSummaryDto {
    private Long id;
    private Long invoiceId;
    private String invoiceNumber;
    private UserMiniDto customerMini;
    private Long customerId;
    private String customerName;
    private Date invoiceDate;
    private InvoiceStatus status; // PAID, PARTIALLY_PAID, PENDING
    private BigDecimal grandTotal;   // Total Bill Amount
    private BigDecimal totalPaid;    // How much received so far
    private BigDecimal balanceDue;   // Remaining to be paid
    private List<InvoicePaymentHistoryDto> paymentHistory;
}
