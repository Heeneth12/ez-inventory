package com.ezh.Inventory.approval.dto;

import com.ezh.Inventory.approval.entity.ApprovalStatus;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalActionDto {
    private Long requestId;
    private ApprovalStatus status; // APPROVED or REJECTED
    private String remarks;
}
