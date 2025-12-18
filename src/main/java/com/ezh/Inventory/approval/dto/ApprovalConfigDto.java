package com.ezh.Inventory.approval.dto;

import com.ezh.Inventory.approval.entity.ApprovalType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalConfigDto {
    private Long id;
    private ApprovalType approvalType;
    private Boolean isEnabled;
    private BigDecimal thresholdAmount;   // For Bill/PO limits
    private Double thresholdPercentage;   // For Discount/Tax limits
    private String approverRole;          // e.g. "MANAGER"
}
