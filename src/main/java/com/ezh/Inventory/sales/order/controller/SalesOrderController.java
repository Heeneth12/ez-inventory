package com.ezh.Inventory.sales.order.controller;

import com.ezh.Inventory.sales.order.dto.SalesConversionReportDto;
import com.ezh.Inventory.sales.order.dto.SalesOrderDto;
import com.ezh.Inventory.sales.order.dto.SalesOrderFilter;
import com.ezh.Inventory.sales.order.dto.SalesOrderStats;
import com.ezh.Inventory.sales.order.entity.SalesOrderStatus;
import com.ezh.Inventory.sales.order.service.SalesOrderService;
import com.ezh.Inventory.utils.common.CommonFilter;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.ResponseResource;
import com.ezh.Inventory.utils.exception.CommonException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/sales/order")
@AllArgsConstructor
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> createSalesOrder(@RequestBody SalesOrderDto salesOrderDto) throws CommonException {
        log.info("Creating new Sales Order: {}", salesOrderDto);
        CommonResponse<?> response = salesOrderService.createSalesOrder(salesOrderDto);
        return ResponseResource.success(HttpStatus.CREATED, response, "Sales Order Created Successfully");
    }

    @PostMapping(value = "/{id}/update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> updateSalesOrder(@PathVariable Long id, @RequestBody SalesOrderDto salesOrderDto) throws CommonException {
        log.info("Updating Sales Order {}: {}", id, salesOrderDto);
        CommonResponse<?> response = salesOrderService.updateSalesOrder(id, salesOrderDto);
        return ResponseResource.success(HttpStatus.OK, response, "Sales Order Updated Successfully");
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<SalesOrderDto> getSalesOrder(@PathVariable Long id) throws CommonException {
        log.info("Fetching Sales Order: {}", id);
        SalesOrderDto response = salesOrderService.getSalesOrderById(id);
        return ResponseResource.success(HttpStatus.OK, response, "Sales Order Fetched Successfully");
    }

    @PostMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<Page<SalesOrderDto>> getAllSalesOrders(@RequestParam Integer page, @RequestParam Integer size,
                                                                   @RequestBody SalesOrderFilter filter) throws CommonException {
        log.info("Fetching All Sales Orders, page: {}, size: {}", page, size);
        Page<SalesOrderDto> response = salesOrderService.getAllSalesOrders(filter, page, size);
        return ResponseResource.success(HttpStatus.OK, response, "Sales Orders Fetched Successfully");
    }

    @PostMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<List<SalesOrderDto>> searchSalesOrders(@RequestBody SalesOrderFilter filter) throws CommonException {
        log.info("Fetching Sales Orders using filters with: {}", filter);
        List<SalesOrderDto> response = salesOrderService.getAllSalesOrders(filter);
        return ResponseResource.success(HttpStatus.OK, response, "Sales Orders Fetched Successfully");
    }

    @PostMapping(value = "/{id}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> updatePrqStatus(@PathVariable Long id,
                                                               @RequestParam SalesOrderStatus status) throws CommonException {
        log.info("Updating status for Sales order ID: {} to {}", id, status);
        CommonResponse<?> response = salesOrderService.updateStatus(id, status);
        return ResponseResource.success(HttpStatus.OK, response, "Sales Orders status updated to " + status);
    }

    @PostMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<SalesOrderStats> getStats(@RequestBody CommonFilter filter) throws CommonException {
        log.info("Updating status for Sales order ID");
        SalesOrderStats response = salesOrderService.getStats(filter);
        return ResponseResource.success(HttpStatus.OK, response, "Sales Orders stats Fetched ");
    }


    @PostMapping(value = "/download", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> downloadSalesOrdersExcel(@RequestBody SalesOrderFilter filter) throws CommonException {
        log.info("Downloading Sales Orders report in excel with filter: {}", filter);
        byte[] response = salesOrderService.downloadSalesOrdersExcel(filter);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales_orders.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(response);
    }

    @PostMapping(value = "/conversion-report", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<SalesConversionReportDto> getSalesOrderConversionReport(@RequestBody CommonFilter filter) throws CommonException {
        log.info("Fetching sales order conversion report with filter: {}", filter);
        SalesConversionReportDto response = salesOrderService.getSalesOrderConversionReport(filter);
        return ResponseResource.success(HttpStatus.OK, response, "Sales order conversion report fetched successfully");
    }


//    @PostMapping(value = "/{id}/delete", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseResource<CommonResponse> deleteSalesOrder(@PathVariable Long id) throws CommonException {
//        log.info("Deleting Sales Order {}", id);
//        CommonResponse response = salesOrderService.deleteSalesOrder(id);
//        return ResponseResource.success(HttpStatus.OK, response, "Sales Order Deleted Successfully");
//    }
//
//    @PostMapping(value = "/{id}/convert-to-invoice", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseResource<CommonResponse> convertToInvoice(@PathVariable Long id) throws CommonException {
//        log.info("Converting Sales Order {} to Invoice", id);
//        CommonResponse response = salesOrderService.convertToInvoice(id);
//        return ResponseResource.success(HttpStatus.OK, response, "Invoice Generated from Sales Order");
//    }
}
