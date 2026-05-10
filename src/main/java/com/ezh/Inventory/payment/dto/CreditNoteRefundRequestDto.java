package com.ezh.Inventory.payment.dto;

import com.ezh.Inventory.payment.entity.enums.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreditNoteRefundRequestDto {
    private Long creditNoteId;
    private BigDecimal refundAmount;
    private PaymentMethod refundMethod;    // how the cash is returned
    private String refundReferenceNumber;
    private String remarks;
}
