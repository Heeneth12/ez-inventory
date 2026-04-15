package com.ezh.Inventory.payment.controller;

import com.ezh.Inventory.payment.dto.AdvanceDto;
import com.ezh.Inventory.payment.dto.CreditNoteDto;
import com.ezh.Inventory.payment.dto.CreditNoteRefundRequestDto;
import com.ezh.Inventory.payment.dto.CreditNoteUtilizeDto;
import com.ezh.Inventory.payment.service.CreditNoteService;
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
@RequestMapping("/v1/credit-note")
@RequiredArgsConstructor
public class CreditNoteController {

    private final CreditNoteService creditNoteService;

    /**
     * Apply a credit note (or part of it) to an invoice
     */
    @PostMapping(value = "/utilize", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> utilizeCreditNote(@RequestBody CreditNoteUtilizeDto dto) throws CommonException {
        log.info("Utilizing credit note {} against invoice {}", dto.getCreditNoteId(), dto.getInvoiceId());
        return ResponseResource.success(HttpStatus.OK, creditNoteService.utilizeCreditNote(dto), "Credit note applied");
    }

    /**
     * Get all advances for a customer
     */
    @PostMapping(value = "/all", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<Page<CreditNoteDto>> getAllCreditNote(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestBody CommonFilter dto) throws CommonException {
        log.info("Getting all advances with filters: {}", dto);
        Page<CreditNoteDto> result = creditNoteService.getAllCreditNote(page, size, dto);
        return ResponseResource.success(HttpStatus.OK, result, "Advances fetched");
    }

    /**
     * Initiate a cash refund of a credit note balance
     */
    @PostMapping(value = "/refund", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> refundCreditNote(@RequestBody CreditNoteRefundRequestDto dto) throws CommonException {
        log.info("Initiating refund for credit note: {}", dto.getCreditNoteId());
        return ResponseResource.success(HttpStatus.OK, creditNoteService.refundCreditNote(dto), "CN refund initiated");
    }

    /**
     * Mark a PENDING CN refund as CLEARED
     */
    @PatchMapping(value = "/refund/{refundId}/confirm")
    public ResponseResource<CommonResponse<?>> confirmRefund(@PathVariable Long refundId) throws CommonException {
        log.info("Confirming CN refund: {}", refundId);
        return ResponseResource.success(HttpStatus.OK, creditNoteService.confirmCreditNoteRefund(refundId), "CN refund cleared");
    }

    /**
     * Get a single credit note with full history
     */
    @GetMapping(value = "/{creditNoteId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CreditNoteDto> getCreditNote(@PathVariable Long creditNoteId) throws CommonException {
        return ResponseResource.success(HttpStatus.OK, creditNoteService.getCreditNote(creditNoteId), "Credit note fetched");
    }

    /**
     * All credit notes for a customer
     */
    @GetMapping(value = "/customer/{customerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<List<CreditNoteDto>> getCreditNotesByCustomer(@PathVariable Long customerId) throws CommonException {
        return ResponseResource.success(HttpStatus.OK, creditNoteService.getCreditNotesByCustomer(customerId), "Credit notes fetched");
    }
}
