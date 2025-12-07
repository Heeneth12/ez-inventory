package com.ezh.Inventory.purchase.returns.controller;

import com.ezh.Inventory.purchase.returns.dto.PurchaseReturnDto;
import com.ezh.Inventory.purchase.returns.service.PurchaseReturnService;
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
@RequestMapping("/v1/purchasereturn")
@RequiredArgsConstructor
public class PurchaseReturnController {

    private final PurchaseReturnService purchaseReturnService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse> createPurchaseReturn(@RequestBody PurchaseReturnDto dto) throws CommonException {
        log.info("Creating Purchase Return: {}", dto);
        CommonResponse response = purchaseReturnService.createPurchaseReturn(dto);
        return ResponseResource.success(HttpStatus.CREATED, response, "Purchase Return created successfully");
    }

    @GetMapping(value = "/{returnId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<PurchaseReturnDto> getReturnDetails(@PathVariable Long returnId) throws CommonException {
        log.info("Fetching Purchase Return details for ID: {}", returnId);
        PurchaseReturnDto dto = purchaseReturnService.getReturnDetails(returnId);
        return ResponseResource.success(HttpStatus.OK, dto, "Purchase Return details fetched");
    }


    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<Page<PurchaseReturnDto>> getAllReturns(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("Fetching Purchase Returns page={} size={}", page, size);
        Page<PurchaseReturnDto> result = purchaseReturnService.getAllReturns(page, size);
        return ResponseResource.success(HttpStatus.OK, result, "Purchase Returns list fetched");
    }
}
