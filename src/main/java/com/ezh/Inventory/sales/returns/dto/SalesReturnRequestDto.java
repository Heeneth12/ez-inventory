package com.ezh.Inventory.sales.returns.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReturnRequestDto {
    private Long invoiceId;
    private String reason;
    private List<ReturnItemRequest> items;
}
