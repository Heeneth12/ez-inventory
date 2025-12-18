package com.ezh.Inventory.approval.dto;

import com.ezh.Inventory.approval.entity.ApprovalResultStatus;

public class ApprovalResult {
    private ApprovalResultStatus status;
    private Long approvalRequestId;
    private String message;
}
