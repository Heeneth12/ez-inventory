package com.ezh.Inventory.sales.returns.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReturnDto {
    private Long id;
    private Long tenantId;
    private String returnNumber;
    private Long invoiceId;
    private Date returnDate;
    private BigDecimal totalAmount;
    private List<SalesReturnItemDto> items;
    private Long creditNotePaymentId; // Nullable
}
