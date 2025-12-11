package com.ezh.Inventory.sales.invoice.controller;


import com.ezh.Inventory.sales.invoice.dto.InvoiceCreateDto;
import com.ezh.Inventory.sales.invoice.dto.InvoiceDto;
import com.ezh.Inventory.sales.invoice.service.InvoiceService;
import com.ezh.Inventory.sales.order.dto.SalesOrderFilter;
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
@RequestMapping("/v1/invoice")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse> createInvoice(@RequestBody InvoiceCreateDto invoiceDto) throws CommonException {
        log.info("Entering createInvoice with : {}", invoiceDto);
        CommonResponse response = invoiceService.createInvoice(invoiceDto);
        return ResponseResource.success(HttpStatus.CREATED, response, "Invoice created successfully");
    }

    @PostMapping(value = "/{invoiceId}/update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse> updateInvoice(@PathVariable Long invoiceId,
                                                          @RequestBody InvoiceCreateDto invoiceDto) throws CommonException {
        log.info("Entering updateInvoice with : {}", invoiceDto);
        CommonResponse response = invoiceService.updateInvoice(invoiceId, invoiceDto);
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
                                                            @RequestBody SalesOrderFilter filter) throws CommonException {
        log.info("get all invoice with page : {} size {}", page, size);
        Page<InvoiceDto> response = invoiceService.getAllInvoices(page, size);
        return ResponseResource.success(HttpStatus.OK, response, "Invoice fetched successfully");
    }

}
