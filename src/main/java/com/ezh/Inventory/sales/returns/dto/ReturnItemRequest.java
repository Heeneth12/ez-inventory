package com.ezh.Inventory.sales.returns.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnItemRequest {
    private Long itemId;
    private Integer quantity;
}
