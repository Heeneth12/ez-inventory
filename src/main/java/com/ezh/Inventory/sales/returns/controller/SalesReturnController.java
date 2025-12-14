package com.ezh.Inventory.sales.returns.controller;


import com.ezh.Inventory.sales.returns.dto.SalesReturnDto;
import com.ezh.Inventory.sales.returns.dto.SalesReturnRequestDto;
import com.ezh.Inventory.sales.returns.service.SalesReturnService;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.ResponseResource;
import com.ezh.Inventory.utils.exception.CommonException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/sales/return")
@AllArgsConstructor
public class SalesReturnController {

    private final SalesReturnService salesReturnService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse> createSalesReturn(@RequestBody SalesReturnRequestDto salesReturnRequestDto) throws CommonException {
        log.info("Creating new Sales return : {}", salesReturnRequestDto);
        CommonResponse response = salesReturnService.createSalesReturn(salesReturnRequestDto);
        return ResponseResource.success(HttpStatus.CREATED, response, "Sales return Created Successfully");
    }

    @PostMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<Page<SalesReturnDto>> getAllSalesReturn(
            @RequestParam Integer page, @RequestParam Integer size) throws CommonException {
        log.info("Fetching Sales Returns page: {}, size: {}", page, size);
        Page<SalesReturnDto> response = salesReturnService.getSalesReturns(page, size);
        return ResponseResource.success(HttpStatus.OK, response, "Sales Returns fetched successfully");
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<SalesReturnDto> getSalesReturnById(@PathVariable Long id) throws CommonException {
        log.info("Fetching Sales Return with ID: {}", id);
        SalesReturnDto response = salesReturnService.getSalesReturnById(id);
        return ResponseResource.success(HttpStatus.OK, response, "Sales Return fetched successfully");
    }

}
