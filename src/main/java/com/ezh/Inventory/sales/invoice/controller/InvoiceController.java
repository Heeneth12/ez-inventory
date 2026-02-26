package com.ezh.Inventory.sales.invoice.controller;


import com.ezh.Inventory.sales.invoice.dto.InvoiceDto;
import com.ezh.Inventory.sales.invoice.dto.InvoiceFilter;
import com.ezh.Inventory.sales.invoice.entity.Invoice;
import com.ezh.Inventory.sales.invoice.repository.InvoiceRepository;
import com.ezh.Inventory.sales.invoice.service.InvoiceService;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.ResponseResource;
import com.ezh.Inventory.utils.common.client.AuthServiceClient;
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
@RequestMapping("/v1/invoice")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final PdfGeneratorService pdfGeneratorService;
    private final InvoiceRepository invoiceRepository;
    private final AuthServiceClient authServiceClient;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> createInvoice(@RequestBody InvoiceDto invoiceDto) throws CommonException {
        log.info("Entering createInvoice with : {}", invoiceDto);
        CommonResponse<?> response = invoiceService.createInvoice(invoiceDto);
        return ResponseResource.success(HttpStatus.CREATED, response, "Invoice created successfully");
    }

    @PostMapping(value = "/{invoiceId}/update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> updateInvoice(@PathVariable Long invoiceId,
                                                          @RequestBody InvoiceDto invoiceDto) throws CommonException {
        log.info("Entering updateInvoice with : {}", invoiceDto);
        CommonResponse<?> response = invoiceService.updateInvoice(invoiceId, invoiceDto);
        return ResponseResource.success(HttpStatus.CREATED, response, "Invoice updated successfully");
    }

    @GetMapping(value = "/{invoiceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<InvoiceDto> getInvoice(@PathVariable Long invoiceId) throws CommonException {
        log.info("getInvoice → {}", invoiceId);
        InvoiceDto response = invoiceService.getInvoiceById(invoiceId);
        return ResponseResource.success(HttpStatus.OK, response, "Invoice fetched successfully");
    }

    @PostMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<Page<InvoiceDto>> getAllInvoice(@RequestParam Integer page, @RequestParam Integer size,
                                                            @RequestBody InvoiceFilter filter) throws CommonException {
        log.info("get all invoice with page : {} size {}", page, size);
        Page<InvoiceDto> response = invoiceService.getAllInvoices(filter, page, size);
        return ResponseResource.success(HttpStatus.OK, response, "Invoice fetched successfully");
    }

    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<List<InvoiceDto>> searchInvoice(@RequestBody InvoiceFilter filter) throws CommonException {
        log.info("Searching invoices with filter: {}", filter);
        List<InvoiceDto> response = invoiceService.searchInvoices(filter);
        return ResponseResource.success(HttpStatus.OK, response, "Invoice fetched successfully");
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id) {
        try {
            Invoice invoice = invoiceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
            Map<Long, UserMiniDto> userMiniDto = authServiceClient.getBulkUserDetails(List.of(invoice.getCustomerId()));
            byte[] pdfBytes = pdfGeneratorService.generateInvoicePdf(invoice, userMiniDto.get(invoice.getCustomerId()));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "Invoice-" + invoice.getInvoiceNumber() + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error generating PDF for invoice {}: {}", id, e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
