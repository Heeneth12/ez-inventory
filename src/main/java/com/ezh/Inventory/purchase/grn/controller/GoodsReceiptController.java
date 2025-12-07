package com.ezh.Inventory.purchase.grn.controller;


import com.ezh.Inventory.purchase.grn.dto.GrnDto;
import com.ezh.Inventory.purchase.grn.service.GoodsReceiptService;
import com.ezh.Inventory.purchase.po.dto.PurchaseOrderDto;
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
@RequestMapping("/v1/grn")
@RequiredArgsConstructor
public class GoodsReceiptController {

    private final GoodsReceiptService goodsReceiptService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse> createAndApproveGrn(@RequestBody GrnDto grnDto) throws CommonException {
        log.info("Creating & approving GRN: {}", grnDto);
        CommonResponse response = goodsReceiptService.createAndApproveGrn(grnDto);
        return ResponseResource.success(HttpStatus.CREATED, response, "GRN created & approved successfully");
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<Page<GrnDto>> getAllPo(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching GRN details page={} size={}", page, size);
        Page<GrnDto> result = goodsReceiptService.getAllGrns(page, size);
        return ResponseResource.success(HttpStatus.OK, result, "Purchase Orders list");
    }

    @GetMapping(value = "/{grnId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<GrnDto> getGrnDetails(@PathVariable Long grnId) throws CommonException {
        log.info("Fetching GRN details for ID: {}", grnId);
        GrnDto result = goodsReceiptService.getGrnDetails(grnId);
        return ResponseResource.success(HttpStatus.OK, result, "GRN details fetched");
    }

    @GetMapping(value = "/po/{poId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<List<GrnDto>> getGrnHistoryForPo(@PathVariable Long poId) {
        log.info("Fetching GRN history for Purchase Order ID: {}", poId);
        List<GrnDto> result = goodsReceiptService.getGrnHistoryForPo(poId);
        return ResponseResource.success(HttpStatus.OK, result, "GRN history fetched for Purchase Order");
    }
}
