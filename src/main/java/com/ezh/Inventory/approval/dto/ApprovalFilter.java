package com.ezh.Inventory.approval.dto;

import com.ezh.Inventory.approval.entity.ApprovalStatus;
import com.ezh.Inventory.approval.entity.ApprovalType;
import com.ezh.Inventory.utils.common.CommonFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalFilter extends CommonFilter {
    private List<ApprovalStatus> approvalStatuses;
    private List<ApprovalType> approvalTypes;
    private String referenceCode;
}
