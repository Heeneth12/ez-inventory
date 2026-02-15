package com.ezh.Inventory.purchase.returns.dto;

import com.ezh.Inventory.purchase.returns.entity.ReturnStatus;
import com.ezh.Inventory.utils.common.dto.UserMiniDto;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseReturnDto {
    private Long id;
    private Long vendorId;
    private Long warehouseId;
    private String prNumber;
    private Long goodsReceiptId; // Optional link
    private String reason;
    private ReturnStatus status;
    private UserMiniDto vendorDetails;
    private List<PurchaseReturnItemDto> items;
}
