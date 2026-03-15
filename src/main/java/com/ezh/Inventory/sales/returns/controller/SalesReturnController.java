package com.ezh.Inventory.sales.returns.controller;


import com.ezh.Inventory.sales.returns.dto.SalesReturnDto;
import com.ezh.Inventory.sales.returns.dto.SalesReturnFilter;
import com.ezh.Inventory.sales.returns.dto.SalesReturnRequestDto;
import com.ezh.Inventory.sales.returns.service.SalesReturnService;
import com.ezh.Inventory.sales.returns.entity.SalesReturn;
import com.ezh.Inventory.sales.returns.repository.SalesReturnRepository;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.ResponseResource;
import com.ezh.Inventory.utils.common.client.AuthServiceClient;
import com.ezh.Inventory.utils.common.dto.TenantDto;
import com.ezh.Inventory.utils.common.dto.UserMiniDto;
import com.ezh.Inventory.utils.exception.CommonException;
import com.ezh.Inventory.utils.pdfsvc.PdfGeneratorService;
import lombok.AllArgsConstructor;
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
@RequestMapping("/v1/sales/return")
@AllArgsConstructor
public class SalesReturnController {

    private final SalesReturnService salesReturnService;
    private final PdfGeneratorService pdfGeneratorService;
    private final SalesReturnRepository salesReturnRepository;
    private final AuthServiceClient authServiceClient;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> createSalesReturn(@RequestBody SalesReturnRequestDto salesReturnRequestDto) throws CommonException {
        log.info("Creating new Sales return : {}", salesReturnRequestDto);
        CommonResponse<?> response = salesReturnService.createSalesReturn(salesReturnRequestDto);
        return ResponseResource.success(HttpStatus.CREATED, response, "Sales return Created Successfully");
    }

    @PostMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<Page<SalesReturnDto>> getAllSalesReturn(
            @RequestParam Integer page, @RequestParam Integer size,
            @RequestBody SalesReturnFilter filter) throws CommonException {
        log.info("Fetching Sales Returns page: {}, size: {}, filter: {}", page, size, filter);
        Page<SalesReturnDto> response = salesReturnService.getSalesReturns(filter, page, size);
        return ResponseResource.success(HttpStatus.OK, response, "Sales Returns fetched successfully");
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<SalesReturnDto> getSalesReturnById(@PathVariable Long id) throws CommonException {
        log.info("Fetching Sales Return with ID: {}", id);
        SalesReturnDto response = salesReturnService.getSalesReturnById(id);
        return ResponseResource.success(HttpStatus.OK, response, "Sales Return fetched successfully");
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadSalesReturnPdf(@PathVariable Long id) {
        try {
            SalesReturn salesReturn = salesReturnRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Sales Return not found"));
            Map<Long, UserMiniDto> userMiniDto = authServiceClient.getBulkUserDetails(List.of(salesReturn.getInvoice().getCustomerId()));
            TenantDto tenant = authServiceClient.getTenantById(salesReturn.getTenantId());

            byte[] pdfBytes = pdfGeneratorService.generateSalesReturnPdf(salesReturn, userMiniDto.get(salesReturn.getInvoice().getCustomerId()), tenant);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "SalesReturn-" + salesReturn.getReturnNumber() + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error generating PDF for sales return {}: {}", id, e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
