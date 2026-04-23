package com.ezh.Inventory.sales.delivery.service;

import com.ezh.Inventory.sales.delivery.dto.*;
import com.ezh.Inventory.sales.invoice.dto.InvoiceDto;
import com.ezh.Inventory.sales.invoice.entity.Invoice;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.exception.CommonException;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DeliveryService {

    Page<DeliveryDto> getAllDeliveries(int page, int size, DeliveryFilterDto filter) throws CommonException;

    DeliveryDto getDeliveryDetail(Long deliveryId) throws CommonException;

    List<DeliveryDto> searchDeliveryDetails(DeliveryFilterDto filter) throws CommonException;

    void createDeliveryForInvoice(Invoice invoice, InvoiceDto dto);

    CommonResponse<?> updateDeliveryStatus(Long id, DeliveryStatusUpdateRequest request, MultipartFile file) throws CommonException;

    CommonResponse<?> createRoute(RouteCreateDto dto) throws CommonException;

    RouteDto getRouteDetail(Long routeId) throws CommonException;

    CommonResponse<?> completeRoute(Long routeId) throws CommonException;

    CommonResponse<?> startRoute(Long routeId) throws CommonException;

    Page<RouteDto> getAllRoutes(int page, int size) throws CommonException;

    RouteSummaryDto getRouteSummary() throws CommonException;

    List<BulkDeliveryItemDto> getBulkDeliveryItems(DeliveryFilterDto filter) throws CommonException;

    byte[] downloadBulkDeliveryItemsExcel(DeliveryFilterDto filter) throws CommonException;
}
