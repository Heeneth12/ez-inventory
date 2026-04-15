package com.ezh.Inventory.payment.dto;

import com.ezh.Inventory.payment.entity.enums.PaymentMethod;
import com.ezh.Inventory.payment.entity.enums.PaymentStatus;
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
    private Long customerId;
    private String customerName;
    private UserMiniDto contactMini;
    private Date paymentDate;
    private BigDecimal amount;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private String referenceNumber;
    private String bankName;
    private String remarks;
    private List<AllocationItem> allocations;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AllocationItem {
        private Long invoiceId;
        private String invoiceNumber;
        private BigDecimal allocatedAmount;
        private Date allocationDate;
    }
}
