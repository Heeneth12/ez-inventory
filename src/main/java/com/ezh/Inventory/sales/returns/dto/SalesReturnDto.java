package com.ezh.Inventory.sales.returns.dto;

import com.ezh.Inventory.utils.common.dto.UserMiniDto;
import com.ezh.Inventory.sales.returns.entity.SalesReturnStatus;
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
    private Long customerId;
    private UserMiniDto contactMini;
    private Date returnDate;
    private BigDecimal totalAmount;
    private SalesReturnStatus status;
    private List<SalesReturnItemDto> items;
    private Long creditNotePaymentId; // Nullable
}
