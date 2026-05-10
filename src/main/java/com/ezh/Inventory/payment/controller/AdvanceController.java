package com.ezh.Inventory.payment.controller;

import com.ezh.Inventory.payment.dto.AdvanceCreateDto;
import com.ezh.Inventory.payment.dto.AdvanceDto;
import com.ezh.Inventory.payment.dto.AdvanceRefundRequestDto;
import com.ezh.Inventory.payment.dto.AdvanceUtilizeDto;
import com.ezh.Inventory.payment.service.AdvanceService;
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

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/advance")
@RequiredArgsConstructor
public class AdvanceController {

    private final AdvanceService advanceService;

    /**
     * Record a new advance deposit from a customer
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> createAdvance(@RequestBody AdvanceCreateDto dto) throws CommonException {
        log.info("Creating advance for customer: {}", dto.getCustomerId());
        return ResponseResource.success(HttpStatus.CREATED, advanceService.createAdvance(dto), "Advance created");
    }


    /**
     * Get all advances for a customer
     */
    @PostMapping(value = "/all", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<Page<AdvanceDto>> getAllAdvances(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestBody CommonFilter dto) throws CommonException {
        log.info("Getting all advances with filters: {}", dto);
        Page<AdvanceDto> result = advanceService.getAllAdvances(page, size, dto);
        return ResponseResource.success(HttpStatus.OK, result, "Advances fetched");
    }

    /**
     * Apply advance (or part of it) to an invoice
     */
    @PostMapping(value = "/utilize", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> utilizeAdvance(@RequestBody AdvanceUtilizeDto dto) throws CommonException {
        log.info("Utilizing advance {} against invoice {}", dto.getAdvanceId(), dto.getInvoiceId());
        return ResponseResource.success(HttpStatus.OK, advanceService.utilizeAdvance(dto), "Advance applied to invoice");
    }

    /**
     * Initiate a cash refund of unused advance balance
     */
    @PostMapping(value = "/refund", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> refundAdvance(@RequestBody AdvanceRefundRequestDto dto) throws CommonException {
        log.info("Initiating refund for advance: {}", dto.getAdvanceId());
        return ResponseResource.success(HttpStatus.OK, advanceService.refundAdvance(dto), "Refund initiated");
    }

    /**
     * Mark a PENDING refund as CLEARED — money reached the customer
     */
    @PatchMapping(value = "/refund/{refundId}/confirm")
    public ResponseResource<CommonResponse<?>> confirmRefund(@PathVariable Long refundId) throws CommonException {
        log.info("Confirming advance refund: {}", refundId);
        return ResponseResource.success(HttpStatus.OK, advanceService.confirmAdvanceRefund(refundId), "Refund cleared");
    }

    /**
     * Get a single advance with full history
     */
    @GetMapping(value = "/{advanceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<AdvanceDto> getAdvance(@PathVariable Long advanceId) throws CommonException {
        return ResponseResource.success(HttpStatus.OK, advanceService.getAdvance(advanceId), "Advance fetched");
    }

    /**
     * All advances for a customer
     */
    @GetMapping(value = "/customer/{customerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<List<AdvanceDto>> getAdvancesByCustomer(@PathVariable Long customerId) throws CommonException {
        return ResponseResource.success(HttpStatus.OK, advanceService.getAdvancesByCustomer(customerId), "Advances fetched");
    }
}
