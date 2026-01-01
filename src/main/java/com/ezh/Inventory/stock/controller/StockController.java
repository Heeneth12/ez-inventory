package com.ezh.Inventory.stock.controller;

import com.ezh.Inventory.stock.dto.*;
import com.ezh.Inventory.stock.service.StockAdjustmentService;
import com.ezh.Inventory.stock.service.StockService;
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
@RequestMapping("/v1/stock")
@CrossOrigin(value = "*")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final StockAdjustmentService stockAdjustmentService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> stockUpdate(@RequestBody StockUpdateDto stockUpdateDto) throws CommonException {
        log.info("Entered Stock Update with : {}", stockUpdateDto);
        CommonResponse<?> response = stockService.updateStock(stockUpdateDto);
        return ResponseResource.success(HttpStatus.CREATED, response, "Stock updated successfully");
    }

    @PostMapping(value = "/all", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<Page<StockDto>> getCurrentStock(@RequestParam Integer page, @RequestParam Integer size,
                                                            @RequestBody StockFilterDto filter) throws CommonException {
        log.info("Entered get current stock with : {}", filter);
        Page<StockDto> response = stockService.getCurrentStock(filter, page, size);
        return ResponseResource.success(HttpStatus.OK, response, "Stock fetched successfully");
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<List<ItemStockSearchDto>> SearchStock(@RequestParam String query, @RequestParam Long warehouseId) throws CommonException {
        log.info("Entered search stock with : {}", query);
        List<ItemStockSearchDto> response = stockService.searchItemsWithBatches(query, warehouseId);
        return ResponseResource.success(HttpStatus.OK, response, "Stock fetched successfully");
    }

    @PostMapping(value = "/ledger", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<Page<StockLedgerDto>> getStockTransactions(@RequestParam Integer page, @RequestParam Integer size,
                                                                       @RequestBody StockFilterDto filter) throws CommonException {
        log.info("Entered get stockTransactions with {}", filter);
        Page<StockLedgerDto> response = stockService.getStockTransactions(filter, page, size);
        return ResponseResource.success(HttpStatus.OK, response, "fetched all stock ledger");
    }

    @PostMapping(path = "/adjustment", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> createStockAdjustment(@RequestBody StockAdjustmentCreateDto stockAdjustmentBatchDto) throws CommonException {
        log.info("Entered Create Stock Adjustment with {}", stockAdjustmentBatchDto);
        CommonResponse<?> response = stockAdjustmentService.createStockAdjustment(stockAdjustmentBatchDto);
        return ResponseResource.success(HttpStatus.CREATED, response, "Stock adjustment successfully");
    }

    @PostMapping(path = "/adjustment/all", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<Page<StockAdjustmentListDto>> getStockAdjustments(@RequestParam Integer page, @RequestParam Integer size,
                                                                              @RequestBody StockFilterDto filter) throws CommonException {
        log.info("Entered get getStockAdjustments with {}", filter);
        Page<StockAdjustmentListDto> response = stockAdjustmentService.getAllStockAdjustments(filter, page, size);
        return ResponseResource.success(HttpStatus.OK, response, "");
    }

    @GetMapping(path = "/adjustment/{adjustmentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<StockAdjustmentDetailDto> getStockAdjustmentById(@PathVariable Long adjustmentId) throws CommonException {
        log.info("Entered get getStockAdjustmentById with {}", adjustmentId);
        StockAdjustmentDetailDto response = stockAdjustmentService.getStockAdjustmentById(adjustmentId);
        return ResponseResource.success(HttpStatus.OK, response, "fetched stock adjustment");
    }

    @GetMapping(path = "/summary/{warehouseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<StockDashboardDto> getStockDashboard(@PathVariable Long warehouseId) throws CommonException {
        log.info("Entered get getStockDashboard with {}", warehouseId);
        StockDashboardDto response = stockService.getStockDashboard(warehouseId);
        return ResponseResource.success(HttpStatus.OK, response, "fetched Stock Dashboard stats");
    }
}
