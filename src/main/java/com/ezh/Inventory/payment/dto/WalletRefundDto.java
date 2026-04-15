package com.ezh.Inventory.payment.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WalletRefundDto {
    private Long customerId;
    private BigDecimal amount;
    private String remarks;
}
