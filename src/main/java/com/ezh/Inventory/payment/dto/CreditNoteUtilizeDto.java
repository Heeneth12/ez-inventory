package com.ezh.Inventory.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreditNoteUtilizeDto {
    private Long creditNoteId; // which specific credit note to draw from
    private Long invoiceId;    // which invoice to apply it to
    private BigDecimal amount;
    private String remarks;
}
