package com.ezh.Inventory.payment.dto;

import com.ezh.Inventory.payment.entity.enums.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdvanceCreateDto {
    private Long customerId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String referenceNumber;
    private String bankName;
    private String remarks;
}
