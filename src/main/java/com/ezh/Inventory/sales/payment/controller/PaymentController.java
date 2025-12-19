package com.ezh.Inventory.sales.payment.controller;


import com.ezh.Inventory.sales.payment.dto.*;
import com.ezh.Inventory.sales.payment.service.PaymentService;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.ResponseResource;
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

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PdfGeneratorService pdfGeneratorService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> recordPayment(@RequestBody PaymentCreateDto paymentCreateDto) throws CommonException {
        log.info("Entering recordPayment with : {}", paymentCreateDto);
        CommonResponse<?> response = paymentService.recordPayment(paymentCreateDto);
        return ResponseResource.success(HttpStatus.CREATED, response, "Payment recorded successfully");
    }

    @PostMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<Page<PaymentDto>> getAllPayments(@RequestBody PaymentFilter filter,  @RequestParam(defaultValue = "0") Integer page,
                                                             @RequestParam(defaultValue = "10") Integer size) throws CommonException {
        log.info("Fetching payments page: {}, size: {}", page, size);
        Page<PaymentDto> response = paymentService.getAllPayments(filter, page, size);
        return ResponseResource.success(HttpStatus.OK, response, "Payments fetched successfully");
    }

    @GetMapping(value = "/invoice/{invoiceId}/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<InvoicePaymentSummaryDto> getPaymentSummary(@PathVariable Long invoiceId) throws CommonException {
        log.info("Entering getPaymentSummary with : {}", invoiceId);
        InvoicePaymentSummaryDto response = paymentService.getInvoicePaymentSummary(invoiceId);
        return ResponseResource.success(HttpStatus.CREATED, response, "Payment summary fetched successfully");
    }

    @GetMapping(value = "/{invoiceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<List<InvoicePaymentHistoryDto>> getPaymentDetails(@PathVariable Long invoiceId) throws CommonException {
        log.info("Entering getPaymentDetails with ID : {}", invoiceId);
        List<InvoicePaymentHistoryDto> response = paymentService.getPaymentsByInvoiceId(invoiceId);
        return ResponseResource.success(HttpStatus.CREATED, response, "Payment recorded successfully");
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<PaymentDto> getPaymentDetailById(@RequestParam Long paymentId) throws CommonException {
        log.info("Entering getPaymentDetailById with ID : {}", paymentId);
        PaymentDto response = paymentService.getPayment(paymentId);
        return ResponseResource.success(HttpStatus.CREATED, response, "Payment recorded successfully");
    }

    @GetMapping(value = "/summary/customer/{customerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CustomerFinancialSummaryDto> getCustomerSummary(@PathVariable Long customerId) throws CommonException {
        log.info("Fetching financial summary for customer: {}", customerId);
        CustomerFinancialSummaryDto response = paymentService.getCustomerFinancialSummary(customerId);
        return ResponseResource.success(HttpStatus.OK, response, "Customer summary fetched successfully");
    }

    @PostMapping(value = "/wallet/refund/{paymentId}")
    public ResponseResource<CommonResponse<?>> refundWalletAmount(@PathVariable Long paymentId,
                                                                  @RequestParam BigDecimal amount) throws CommonException {
        log.info("Refunding amount {} from paymentId {}", amount, paymentId);
        CommonResponse<?> response = paymentService.refundUnallocatedAmount(paymentId, amount);
        return ResponseResource.success(HttpStatus.OK, response, "Refund processed successfully");
    }

    @PostMapping(value = "/wallet/apply", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> applyWalletToInvoice(@RequestBody WalletPayDto walletPayDto) throws CommonException {
        log.info("Applying wallet funds: {}", walletPayDto);
        CommonResponse<?> response = paymentService.applyWalletToInvoice(walletPayDto);
        return ResponseResource.success(HttpStatus.OK, response, "Wallet balance applied successfully");
    }

    /**
     * Generate and Download Payment PDF
     */
    @GetMapping(value = "/{paymentId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadPaymentPdf(@PathVariable Long paymentId) throws Exception {
        log.info("Generating PDF for payment: {}", paymentId);
        // Assuming you call your service which calls PdfGeneratorService
        PaymentDto paymentDto = paymentService.getPayment(paymentId);
        byte[] pdfContent = pdfGeneratorService.generatePaymentPdf(paymentDto);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Receipt-" + paymentId + ".pdf");

        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }
}
