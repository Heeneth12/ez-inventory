package com.ezh.Inventory.payment.controller;

import com.ezh.Inventory.payment.dto.*;
import com.ezh.Inventory.payment.entity.Payment;
import com.ezh.Inventory.payment.repository.PaymentRepository;
import com.ezh.Inventory.payment.service.PaymentService;
import com.ezh.Inventory.utils.common.CommonFilter;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.ResponseResource;
import com.ezh.Inventory.utils.common.client.AuthServiceClient;
import com.ezh.Inventory.utils.common.dto.TenantDto;
import com.ezh.Inventory.utils.common.dto.UserMiniDto;
import com.ezh.Inventory.utils.exception.CommonException;
import com.ezh.Inventory.utils.pdfsvc.PdfGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final AuthServiceClient authServiceClient;

    /**
     * Record a payment against one or more invoices. Sum of allocations must equal total amount.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> recordPayment(@RequestBody PaymentCreateDto dto) throws CommonException {
        log.info("Recording payment for customer: {}", dto.getCustomerId());
        return ResponseResource.success(HttpStatus.CREATED, paymentService.recordPayment(dto), "Payment recorded");
    }

    @PostMapping(value = "/all")
    public ResponseResource<Page<PaymentDto>> getAllPayments(
            @RequestBody PaymentFilter filter,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) throws CommonException {
        return ResponseResource.success(HttpStatus.OK, paymentService.getAllPayments(filter, page, size), "Payments fetched");
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<PaymentDto> getPayment(@RequestParam Long paymentId) throws CommonException {
        return ResponseResource.success(HttpStatus.OK, paymentService.getPayment(paymentId), "Payment fetched");
    }

    @GetMapping(value = "/{invoiceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<List<InvoicePaymentHistoryDto>> getPaymentsByInvoice(@PathVariable Long invoiceId) throws CommonException {
        return ResponseResource.success(HttpStatus.OK, paymentService.getPaymentsByInvoiceId(invoiceId), "Payment history fetched");
    }

    @GetMapping(value = "/invoice/{invoiceId}/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<InvoicePaymentSummaryDto> getInvoiceSummary(@PathVariable Long invoiceId) throws CommonException {
        return ResponseResource.success(HttpStatus.OK, paymentService.getInvoicePaymentSummary(invoiceId), "Invoice summary fetched");
    }

    /**
     * Customer financial summary — shows outstanding invoices, advance balance, and credit note balance.
     * Advance and CN details are available via /v1/advance and /v1/credit-note endpoints.
     */
    @GetMapping(value = "/summary/customer/{customerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CustomerFinancialSummaryDto> getCustomerSummary(@PathVariable Long customerId) throws CommonException {
        return ResponseResource.success(HttpStatus.OK, paymentService.getCustomerFinancialSummary(customerId), "Customer summary fetched");
    }

    @PostMapping(value = "/stats")
    public ResponseResource<PaymentStats> getStats(@RequestBody CommonFilter filter) throws CommonException {
        return ResponseResource.success(HttpStatus.OK, paymentService.getStats(filter), "Stats fetched");
    }

    @GetMapping(value = "/{paymentId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadPaymentPdf(@PathVariable Long paymentId) throws Exception {
        log.info("Generating PDF for payment: {}", paymentId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        Map<Long, UserMiniDto> userMap = authServiceClient.getBulkUserDetails(List.of(payment.getCustomerId()));
        TenantDto tenant = authServiceClient.getTenantById(payment.getTenantId());
        byte[] pdf = pdfGeneratorService.generatePaymentPdf(payment, userMap.get(payment.getCustomerId()), tenant);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Receipt-" + paymentId + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
