package com.ezh.Inventory.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalStatsDto {
    private Long totalCount;
    private Long approvedCount;
    private Long pendingCount;
    private Long rejectedCount;
}
