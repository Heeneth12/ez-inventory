package com.ezh.Inventory.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdvanceUtilizeDto {
    private Long advanceId;   // which specific advance to draw from
    private Long invoiceId;   // which invoice to pay
    private BigDecimal amount; // how much to apply
    private String remarks;
}
