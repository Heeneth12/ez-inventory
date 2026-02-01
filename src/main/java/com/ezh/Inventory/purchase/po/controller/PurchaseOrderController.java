package com.ezh.Inventory.purchase.po.controller;

import com.ezh.Inventory.purchase.po.dto.PurchaseOrderDto;
import com.ezh.Inventory.purchase.po.dto.PurchaseOrderFilter;
import com.ezh.Inventory.purchase.po.entity.PoStatus;
import com.ezh.Inventory.purchase.po.service.PurchaseOrderService;
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
@RequestMapping("/v1/purchaseorder")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse> createPo(@RequestBody PurchaseOrderDto purchaseOrderDto) throws CommonException {
        log.info("Entering create purchase order with : {}", purchaseOrderDto);
        CommonResponse response = purchaseOrderService.createPurchaseOrder(purchaseOrderDto);
        return ResponseResource.success(HttpStatus.CREATED, response, "Purchase Order created successfully");
    }

    @GetMapping(value = "/{poId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<PurchaseOrderDto> getPoById(@PathVariable Long poId) throws CommonException {
        log.info("Entering Fetching Purchase Order with ID: {}", poId);
        PurchaseOrderDto dto = purchaseOrderService.getPurchaseOrderById(poId);
        return ResponseResource.success(HttpStatus.OK, dto, "Purchase Order details");
    }

    @PostMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<Page<PurchaseOrderDto>> getAllPo(@RequestBody PurchaseOrderFilter filter,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching Purchase Orders page={} size={}", page, size);
        Page<PurchaseOrderDto> result = purchaseOrderService.getAllPurchaseOrders(page, size, filter);
        return ResponseResource.success(HttpStatus.OK, result, "Purchase Orders list");
    }

    @PostMapping(value = "/{poId}/update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse> updatePo(@PathVariable Long poId,
                                                     @RequestBody PurchaseOrderDto purchaseOrderDto)
            throws CommonException {
        log.info("Updating Purchase Order ID: {} with {}", poId, purchaseOrderDto);
        CommonResponse response = purchaseOrderService.updatePurchaseOrder(poId, purchaseOrderDto);
        return ResponseResource.success(HttpStatus.OK, response, "Purchase Order updated successfully");
    }

    @PostMapping(value = "/{poId}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse> updatePoStatus(@PathVariable Long poId, @RequestParam PoStatus status) throws CommonException {
        log.info("Updating status for Purchase Order ID: {} to {}", poId, status);
        CommonResponse response = purchaseOrderService.updatePurchaseOrderStatus(poId, status);
        return ResponseResource.success(HttpStatus.OK, response, "Purchase Order status updated to " + status);
    }
}