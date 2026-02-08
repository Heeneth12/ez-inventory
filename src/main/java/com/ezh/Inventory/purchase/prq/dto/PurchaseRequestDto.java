package com.ezh.Inventory.purchase.prq.dto;


import com.ezh.Inventory.purchase.prq.entity.PrqStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseRequestDto {
    private Long id;
    private Long vendorId;
    private String vendorName;
    private Long warehouseId;
    private Long requestedBy;
    private String department;
    private String prqNumber;
    private PrqStatus status;
    private BigDecimal totalEstimatedAmount;
    private String notes;
    private Date createdAt;
    private List<PurchaseRequestItemDto> items;
}
