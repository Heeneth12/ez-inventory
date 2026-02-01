package com.ezh.Inventory.purchase.prq.controller;

import com.ezh.Inventory.purchase.prq.dto.PurchaseRequestDto;
import com.ezh.Inventory.purchase.prq.entity.PrqStatus;
import com.ezh.Inventory.purchase.prq.service.PurchaseRequestService;
import com.ezh.Inventory.utils.common.CommonFilter;
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
@RequestMapping("/v1/prq")
@RequiredArgsConstructor
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> createPrq(@RequestBody PurchaseRequestDto purchaseRequestDto) throws CommonException {
        log.info("Entering create purchase request with : {}", purchaseRequestDto);
        CommonResponse<?> response = purchaseRequestService.createPrq(purchaseRequestDto);
        return ResponseResource.success(HttpStatus.CREATED, response, "Purchase Request created successfully");
    }

    @GetMapping(value = "/{prqId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<PurchaseRequestDto> getPrqById(@PathVariable Long prqId) throws CommonException {
        log.info("Entering Fetching Purchase Request with ID: {}", prqId);
        PurchaseRequestDto dto = purchaseRequestService.getPrqById(prqId);
        return ResponseResource.success(HttpStatus.OK, dto, "Purchase Request details");
    }

    @PostMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<Page<PurchaseRequestDto>> getAllPrq(@RequestBody CommonFilter filter,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size) throws CommonException {
        log.info("Fetching Purchase Requests page={} size={} with filter={}", page, size, filter);
        Page<PurchaseRequestDto> result = purchaseRequestService.getAllPrqs(page, size, filter);
        return ResponseResource.success(HttpStatus.OK, result, "Purchase Requests list");
    }

    @PostMapping(value = "/{prqId}/update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> updatePrq(@PathVariable Long prqId,
                                                         @RequestBody PurchaseRequestDto purchaseRequestDto)
            throws CommonException {
        log.info("Updating Purchase Request ID: {} with {}", prqId, purchaseRequestDto);
        CommonResponse<?> response = purchaseRequestService.updatePrq(prqId, purchaseRequestDto);
        return ResponseResource.success(HttpStatus.OK, response, "Purchase Request updated successfully");
    }

    @PostMapping(value = "/{prqId}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> updatePrqStatus(@PathVariable Long prqId,
                                                               @RequestParam PrqStatus status) throws CommonException {
        log.info("Updating status for Purchase Request ID: {} to {}", prqId, status);
        CommonResponse<?> response = purchaseRequestService.updateStatus(prqId, status);
        return ResponseResource.success(HttpStatus.OK, response, "Purchase Request status updated to " + status);
    }
}