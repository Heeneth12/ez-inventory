package com.ezh.Inventory.approval.controller;


import com.ezh.Inventory.approval.dto.ApprovalActionDto;
import com.ezh.Inventory.approval.dto.ApprovalConfigDto;
import com.ezh.Inventory.approval.dto.ApprovalRequestDto;
import com.ezh.Inventory.approval.entity.ApprovalType;
import com.ezh.Inventory.approval.service.ApprovalService;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.ResponseResource;
import com.ezh.Inventory.utils.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/approval")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;


    @PostMapping(value = "/all", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<Page<ApprovalRequestDto>> getAllApprovals(@RequestParam(defaultValue = "0") Integer page,
                                                                      @RequestParam(defaultValue = "10") Integer size) throws CommonException {
        log.info("Fetching approvals page={} size={}", page, size);
        Page<ApprovalRequestDto> response = approvalService.getAllApprovals(page, size);
        return ResponseResource.success(HttpStatus.OK, response, "Approvals fetched successfully");
    }

    @PostMapping(value = "/process", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> processApproval(@RequestBody ApprovalActionDto approvalActionDto) throws CommonException {
        log.info("Processing approval request: {}", approvalActionDto);
        CommonResponse<?> response = approvalService.processDecision(approvalActionDto);
        return ResponseResource.success(HttpStatus.OK, response, "Approval processed successfully");
    }

    @GetMapping(value = "/{approvalRequestId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<ApprovalRequestDto> getApprovalById(@PathVariable Long approvalRequestId) throws CommonException {
        log.info("Fetching approval request by id : {}", approvalRequestId);
        ApprovalRequestDto response = approvalService.getApprovalById(approvalRequestId);
        return ResponseResource.success(HttpStatus.OK, response, "Approval request fetched successfully");
    }

    @PostMapping(value = "/config", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> createApprovalConfig(@RequestBody ApprovalConfigDto approvalConfigDto) throws CommonException {
        log.info("Entering Creating approval config : {}", approvalConfigDto);
        CommonResponse<?> response = approvalService.createOrUpdateConfig(approvalConfigDto);
        return ResponseResource.success(HttpStatus.CREATED, response, "Approval config saved successfully");
    }

    @PostMapping(value = "/config/all", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<Page<ApprovalConfigDto>> getAllApprovalConfig(@RequestParam(defaultValue = "0") Integer page,
                                                                          @RequestParam(defaultValue = "10") Integer size) throws CommonException {
        log.info("Fetching approvals config page={} size={}", page, size);
        Page<ApprovalConfigDto> response = approvalService.getAllConfigs(page, size);
        return ResponseResource.success(HttpStatus.OK, response, "Approval config fetched successfully");
    }

    @GetMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<ApprovalConfigDto> getApprovalConfig(@RequestParam(required = false) Long approvalConfigId,
                                                                 @RequestParam(required = false) ApprovalType approvalType) throws CommonException {
        log.info("Fetching approval config by id={} type={}", approvalConfigId, approvalType);
        ApprovalConfigDto response = approvalConfigId != null
                ? approvalService.getConfigById(approvalConfigId)
                : approvalService.getConfigApprovalType(approvalType);
        return ResponseResource.success(HttpStatus.OK, response, "Approval config fetched successfully");
    }

}
