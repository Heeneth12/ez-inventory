package com.ezh.Inventory.utils.common.events;

import com.ezh.Inventory.approval.entity.ApprovalStatus;
import com.ezh.Inventory.approval.entity.ApprovalType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ApprovalDecisionEvent extends ApplicationEvent {

    private final ApprovalType type;
    private final Long referenceId;
    private final ApprovalStatus status;

    public ApprovalDecisionEvent(Object source, ApprovalType type, Long referenceId, ApprovalStatus status) {
        super(source);
        this.type = type;
        this.referenceId = referenceId;
        this.status = status;
    }
}