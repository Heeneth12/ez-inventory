package com.ezh.Inventory.payment.dto;

import com.ezh.Inventory.payment.entity.enums.AdvanceStatus;
import com.ezh.Inventory.payment.entity.enums.PaymentMethod;
import com.ezh.Inventory.payment.entity.enums.RefundStatus;
import com.ezh.Inventory.payment.entity.enums.UtilizationStatus;
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
public class AdvanceDto {
    private Long id;
    private String advanceNumber;
    private Long customerId;
    private String customerName;
    private UserMiniDto contactMini;
    private Date receivedDate;
    private BigDecimal amount;
    private BigDecimal availableBalance;
    private PaymentMethod paymentMethod;
    private AdvanceStatus status;
    private String referenceNumber;
    private String bankName;
    private String remarks;

    // Populated only in detail view
    private List<UtilizationItem> utilizations;
    private List<RefundItem> refunds;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UtilizationItem {
        private Long id;
        private Long invoiceId;
        private String invoiceNumber;
        private BigDecimal utilizedAmount;
        private Date utilizationDate;
        private UtilizationStatus status;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundItem {
        private Long id;
        private String refundNumber;
        private BigDecimal refundAmount;
        private Date refundDate;
        private PaymentMethod refundMethod;
        private String refundReferenceNumber;
        private RefundStatus status;
    }
}
