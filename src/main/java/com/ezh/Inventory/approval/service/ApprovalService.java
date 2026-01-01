package com.ezh.Inventory.approval.service;

import com.ezh.Inventory.approval.dto.ApprovalActionDto;
import com.ezh.Inventory.approval.dto.ApprovalCheckContext;
import com.ezh.Inventory.approval.dto.ApprovalConfigDto;
import com.ezh.Inventory.approval.dto.ApprovalRequestDto;
import com.ezh.Inventory.approval.entity.*;
import com.ezh.Inventory.approval.repository.ApprovalConfigRepository;
import com.ezh.Inventory.approval.repository.ApprovalRequestRepository;
import com.ezh.Inventory.utils.UserContextUtil;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.Status;
import com.ezh.Inventory.utils.common.events.ApprovalDecisionEvent;
import com.ezh.Inventory.utils.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalConfigRepository approvalConfigRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CommonResponse<?> checkAndInitiateApproval(ApprovalCheckContext context) {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        // 1. Fetch the Rule for this specific type (e.g., INVOICE_DISCOUNT)
        Optional<ApprovalConfig> configOpt = approvalConfigRepository.findByTenantIdAndApprovalType(tenantId, context.getType());

        // If no rule exists or rule is disabled -> Auto Approve
        if (configOpt.isEmpty() || !configOpt.get().isEnabled()) {
            return CommonResponse.builder()
                    .status(Status.SUCCESS)
                    .data(ApprovalResultStatus.AUTO_APPROVED)
                    .message("No approval required")
                    .build();
        }

        ApprovalConfig config = configOpt.get();
        boolean approvalNeeded = false;
        String reason = "";

        // 2. Compare Incoming Data vs Config Rules
        switch (context.getType()) {
            // Case A: Percentage Checks (Discount, Tax Variance)
            case INVOICE_DISCOUNT:
            case SALES_ORDER_DISCOUNT:
            case TAX_VARIANCE:
                if (context.getPercentage() != null && config.getThresholdPercentage() != null) {
                    if (context.getPercentage() > config.getThresholdPercentage()) {
                        approvalNeeded = true;
                        reason = String.format("%s of %.2f%% exceeds limit of %.2f%%",
                                context.getType(), context.getPercentage(), config.getThresholdPercentage());
                    }
                }
                break;

            // Case B: Amount Checks (High Value Invoice, PO Limit, Stock Adjustment)
            case HIGH_VALUE_INVOICE:
            case PO_APPROVAL:
            case STOCK_ADJUSTMENT:
                if (context.getAmount() != null && config.getThresholdAmount() != null) {
                    if (context.getAmount().compareTo(config.getThresholdAmount()) > 0) {
                        approvalNeeded = true;
                        reason = String.format("Amount %s exceeds limit of %s",
                                context.getAmount(), config.getThresholdAmount());
                    }
                }
                break;

            // Case C: Absolute Checks (Always require approval if enabled)
            case SALES_REFUND:
            case ADVANCE_REFUND:
                approvalNeeded = true;
                reason = "Refund/Return operation requires manager approval.";
                break;
        }

        // 3. If Rule Broken -> Create Request
        if (approvalNeeded) {

            ApprovalRequest approvalRequest = createRequest(context, reason);

            return CommonResponse.builder()
                    .data(ApprovalResultStatus.APPROVAL_REQUIRED)
                    .id(approvalRequest.getId().toString())
                    .message("Approval request created and pending")
                    .build();
        }

        return CommonResponse.builder()
                .data(ApprovalResultStatus.AUTO_APPROVED)
                .message("Approval rules satisfied")
                .build();
    }

    /**
     * ACTION LOGIC: Admin Approves or Rejects a request.
     */
    @Transactional
    public CommonResponse<?> processDecision(ApprovalActionDto actionDto) throws CommonException{

        // 1. Fetch the Request
        ApprovalRequest request = approvalRequestRepository.findById(actionDto.getRequestId())
                .orElseThrow(() -> new CommonException("Request not found", HttpStatus.NOT_FOUND));

        // 2. Update the Request Log
        request.setStatus(actionDto.getStatus()); // APPROVED or REJECTED
        request.setActionedBy(UserContextUtil.getUserId());
        request.setActionRemarks(actionDto.getRemarks());
        approvalRequestRepository.save(request);

        // 2. Publish the Event instead of calling services directly
        ApprovalDecisionEvent event = new ApprovalDecisionEvent(
                this,
                request.getApprovalType(),
                request.getReferenceId(),
                actionDto.getStatus()
        );

        eventPublisher.publishEvent(event);

        return CommonResponse.builder()
                .message("Request processed successfully")
                .build();
    }


    @Transactional(readOnly = true)
    public ApprovalRequestDto getApprovalById(Long approvalRequestId) throws CommonException {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        ApprovalRequest approvalRequest =
                approvalRequestRepository
                        .findByIdAndTenantId(approvalRequestId, tenantId)
                        .orElseThrow(() ->
                                new CommonException("Approval request not found", HttpStatus.NOT_FOUND)
                        );

        return toDto(approvalRequest);
    }


    @Transactional(readOnly = true)
    public Page<ApprovalRequestDto> getAllApprovals(Integer page, Integer size) throws CommonException {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ApprovalRequest> approvalRequests =
                approvalRequestRepository.findByTenantId(tenantId, pageable);

        return approvalRequests.map(ApprovalService::toDto);
    }


    @Transactional
    public CommonResponse<?> createOrUpdateConfig(ApprovalConfigDto dto) throws CommonException {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        // 1. Check if a config for this Type already exists for this Tenant
        Optional<ApprovalConfig> existing = approvalConfigRepository.findByTenantIdAndApprovalType(
                tenantId, dto.getApprovalType());

        ApprovalConfig config;
        if (existing.isPresent()) {
            config = existing.get(); // Update existing rule
        } else {
            config = new ApprovalConfig(); // Create new rule
            config.setTenantId(tenantId);
            config.setApprovalType(dto.getApprovalType());
        }
        // 2. Set the Limits
        config.setEnabled(dto.getIsEnabled());
        config.setThresholdAmount(dto.getThresholdAmount());       // e.g., 100000.00
        config.setThresholdPercentage(dto.getThresholdPercentage()); // e.g., 10.0
        config.setApproverRole(dto.getApproverRole());             // e.g., "MANAGER"

        approvalConfigRepository.save(config);

        return CommonResponse
                .builder()
                .status(Status.SUCCESS)
                .message("Created successfully")
                .build();
    }


    @Transactional(readOnly = true)
    public ApprovalConfigDto getConfigById(Long approvalConfigId) throws CommonException {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        ApprovalConfig approvalConfig =
                approvalConfigRepository
                        .findByIdAndTenantId(approvalConfigId, tenantId)
                        .orElseThrow(() ->
                                new CommonException("Approval config not found", HttpStatus.NOT_FOUND)
                        );

        return toDto(approvalConfig);
    }

    @Transactional(readOnly = true)
    public ApprovalConfigDto getConfigApprovalType(ApprovalType approvalType) throws CommonException {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        ApprovalConfig approvalConfig =
                approvalConfigRepository
                        .findByApprovalTypeAndTenantId(approvalType, tenantId)
                        .orElseThrow(() ->
                                new CommonException("Approval config not found", HttpStatus.NOT_FOUND)
                        );

        return toDto(approvalConfig);
    }


    @Transactional(readOnly = true)
    public Page<ApprovalConfigDto> getAllConfigs(Integer page, Integer size) throws CommonException {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Pageable pageable = PageRequest.of(page, size);

        Page<ApprovalConfig> approvalConfigs =
                approvalConfigRepository.findByTenantId(tenantId, pageable);

        return approvalConfigs.map(ApprovalService::toDto);
    }

    @Transactional(readOnly = true)
    private ApprovalRequest createRequest(ApprovalCheckContext context, String reason) {
        ApprovalRequest request = ApprovalRequest.builder()
                .tenantId(UserContextUtil.getTenantIdOrThrow())
                .approvalType(context.getType())
                .referenceId(context.getReferenceId())
                .referenceCode(context.getReferenceCode())
                .requestedBy(UserContextUtil.getUserId())
                .status(ApprovalStatus.PENDING)
                .description(reason)
                // Store the value that triggered it (Amount OR Percentage)
                .valueAmount(context.getAmount() != null ? context.getAmount() : BigDecimal.valueOf(context.getPercentage()))
                .build();

        return approvalRequestRepository.save(request);
    }


    private static ApprovalConfigDto toDto(ApprovalConfig entity) {

        if (entity == null) {
            return null;
        }

        ApprovalConfigDto dto = new ApprovalConfigDto();
        dto.setId(entity.getId());
        dto.setApprovalType(entity.getApprovalType());
        dto.setIsEnabled(entity.isEnabled());
        dto.setThresholdAmount(entity.getThresholdAmount());
        dto.setThresholdPercentage(entity.getThresholdPercentage());
        dto.setApproverRole(entity.getApproverRole());

        return dto;
    }


    public static ApprovalRequestDto toDto(ApprovalRequest entity) {

        if (entity == null) {
            return null;
        }

        return ApprovalRequestDto.builder()
                .id(entity.getId())
                .approvalType(entity.getApprovalType())
                .referenceId(entity.getReferenceId())
                .referenceCode(entity.getReferenceCode())
                .status(entity.getStatus())
                .requestedBy(entity.getRequestedBy())
                .description(entity.getDescription())
                .valueAmount(entity.getValueAmount())
                .actionedBy(entity.getActionedBy())
                .actionRemarks(entity.getActionRemarks())
                .createdAt(
                        entity.getCreatedAt() != null
                                ? entity.getCreatedAt().toString()
                                : null
                )
                .build();
    }

}