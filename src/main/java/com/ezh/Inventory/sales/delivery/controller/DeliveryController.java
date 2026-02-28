package com.ezh.Inventory.sales.delivery.controller;


import com.ezh.Inventory.sales.delivery.dto.*;
import com.ezh.Inventory.sales.delivery.entity.ShipmentStatus;
import com.ezh.Inventory.sales.delivery.service.DeliveryService;
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
@RequestMapping("/v1/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping(value = "/{deliveryId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<DeliveryDto> getDeliveryDetail(@PathVariable Long deliveryId) throws CommonException {
        log.info("Fetching delivery details for id: {}", deliveryId);
        DeliveryDto response = deliveryService.getDeliveryDetail(deliveryId);
        return ResponseResource.success(HttpStatus.OK, response, "Delivery details fetched successfully");
    }

    @PostMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<Page<DeliveryDto>> getDeliveryDetails(@RequestParam Integer page, @RequestParam Integer size,
                                                                  @RequestBody DeliveryFilterDto filter) throws CommonException {
        log.info("Fetching all deliveries with page: {} and size: {}", page, size);
        Page<DeliveryDto> response = deliveryService.getAllDeliveries(page, size, filter);
        return ResponseResource.success(HttpStatus.OK, response, "Deliveries fetched successfully");
    }

    @PostMapping(value = "/{deliveryId}/delivered", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse> markAsDelivered(@PathVariable Long deliveryId) throws CommonException {
        log.info("Marking delivery {} as delivered", deliveryId);
        CommonResponse response = deliveryService.markAsDelivered(deliveryId);
        return ResponseResource.success(HttpStatus.OK, response, "Delivery marked as delivered successfully");
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<List<DeliveryDto>> searchDeliveryDetails(@RequestBody DeliveryFilterDto filter) throws CommonException {
        log.info("Searching deliveries with filter: {}", filter);
        List<DeliveryDto> response = deliveryService.searchDeliveryDetails(filter);
        return ResponseResource.success(HttpStatus.OK, response, "Deliveries fetched successfully based on search criteria");
    }

    @PostMapping(value = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> updateDeliveryStatus(@PathVariable Long id,
                                                                    @RequestParam ShipmentStatus status) throws CommonException {
        log.info("Update invoices with status: {}", status);
        CommonResponse<?> response = deliveryService.updateDeliveryStatus(id, status);
        return ResponseResource.success(HttpStatus.OK, response, "Delivery status updated successfully");
    }

    @PostMapping(value = "/route", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> createRoute(@RequestBody RouteCreateDto routeCreateDto) throws CommonException {
        log.info("Entering createRoute for area: {}", routeCreateDto.getAreaName());
        CommonResponse<?> response = deliveryService.createRoute(routeCreateDto);
        return ResponseResource.success(HttpStatus.CREATED, response, "Route manifest created successfully");
    }

    @GetMapping(value = "/route/{routeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<RouteDto> getRouteDetail(@PathVariable Long routeId) throws CommonException {
        log.info("Fetching route details for ID: {}", routeId);
        RouteDto response = deliveryService.getRouteDetail(routeId);
        return ResponseResource.success(HttpStatus.OK, response, "Route details fetched successfully");
    }

    @PostMapping(value = "/route/start/{routeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> startRoute(@PathVariable Long routeId) throws CommonException {
        log.info("Starting route trip for ID: {}", routeId);
        CommonResponse<?> response = deliveryService.startRoute(routeId);
        return ResponseResource.success(HttpStatus.OK, response, "Trip started successfully");
    }

    @PostMapping(value = "/route/complete/{routeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> completeRoute(@PathVariable Long routeId) throws CommonException {
        log.info("Completing route manifest for ID: {}", routeId);
        CommonResponse<?> response = deliveryService.completeRoute(routeId);
        return ResponseResource.success(HttpStatus.OK, response, "Route manifest closed successfully");
    }

    @GetMapping(value = "/route/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<Page<RouteDto>> getAllRoutes(@RequestParam(defaultValue = "0") Integer page,
                                                         @RequestParam(defaultValue = "10") Integer size) throws CommonException {
        log.info("Fetching routes page: {}, size: {}", page, size);
        Page<RouteDto> response = deliveryService.getAllRoutes(page, size);
        return ResponseResource.success(HttpStatus.OK, response, "Routes fetched successfully");
    }

    @GetMapping(value = "/route/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<RouteSummaryDto> getRouteSummary() throws CommonException {
        log.info("Fetching delivery and route summary dashboard stats");
        RouteSummaryDto response = deliveryService.getRouteSummary();
        return ResponseResource.success(HttpStatus.OK, response, "Summary stats fetched successfully");
    }
}
