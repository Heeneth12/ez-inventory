package com.ezh.Inventory.payment.dto;

import com.ezh.Inventory.payment.entity.enums.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdvanceRefundRequestDto {
    private Long advanceId;                // which advance to refund from
    private BigDecimal refundAmount;
    private PaymentMethod refundMethod;    // how the cash is returned (CASH, CHEQUE, UPI, ...)
    private String refundReferenceNumber;  // cheque no / UTR (optional)
    private String remarks;
}
