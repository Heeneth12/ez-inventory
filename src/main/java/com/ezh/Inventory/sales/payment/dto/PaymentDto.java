package com.ezh.Inventory.sales.payment.dto;

import com.ezh.Inventory.sales.invoice.dto.InvoiceMiniDto;
import com.ezh.Inventory.sales.payment.entity.PaymentMethod;
import com.ezh.Inventory.sales.payment.entity.PaymentStatus;
import com.ezh.Inventory.utils.common.dto.UserMiniDto;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDto {
    private Long id;
    private Long tenantId;
    private String paymentNumber;
    private UserMiniDto contactMini;
    private Long customerId;
    private String customerName;
    private Date paymentDate;
    private BigDecimal amount;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private String referenceNumber;
    private String bankName;
    private String remarks;
    private List<InvoiceMiniDto> invoices;
    private BigDecimal allocatedAmount = BigDecimal.ZERO;
    private BigDecimal unallocatedAmount = BigDecimal.ZERO;
}
