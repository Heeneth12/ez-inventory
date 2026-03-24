package com.ezh.Inventory.sales.delivery.service;

import com.ezh.Inventory.sales.delivery.dto.*;
import com.ezh.Inventory.sales.delivery.entity.ShipmentStatus;
import com.ezh.Inventory.sales.invoice.dto.InvoiceDto;
import com.ezh.Inventory.sales.invoice.entity.Invoice;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.exception.CommonException;
import org.springframework.data.domain.Page;

import java.util.Date;
import java.util.List;

public interface DeliveryService {

    CommonResponse createDeliveryFromInvoice(Long invoiceId) throws CommonException;

    Page<DeliveryDto> getAllDeliveries(int page, int size, DeliveryFilterDto filter) throws CommonException;

    DeliveryDto getDeliveryDetail(Long deliveryId) throws CommonException;

    List<DeliveryDto> searchDeliveryDetails(DeliveryFilterDto filter) throws CommonException;

    void createDeliveryForInvoice(Invoice invoice, InvoiceDto dto);

    CommonResponse<?> markAsShipped(Long deliveryId, String trackingNumber);

    CommonResponse<?> markAsDelivered(Long deliveryId);

    CommonResponse<?> cancelDelivery(Long deliveryId, String reason) throws CommonException;

    CommonResponse<?> rescheduleDelivery(Long deliveryId, Date newDate, String reason) throws CommonException;

    CommonResponse<?> updateDeliveryStatus(Long id, ShipmentStatus status) throws CommonException;

    CommonResponse<?> createRoute(RouteCreateDto dto) throws CommonException;

    RouteDto getRouteDetail(Long routeId) throws CommonException;

    CommonResponse<?> completeRoute(Long routeId) throws CommonException;

    CommonResponse<?> startRoute(Long routeId) throws CommonException;

    Page<RouteDto> getAllRoutes(int page, int size) throws CommonException;

    RouteSummaryDto getRouteSummary() throws CommonException;

    List<BulkDeliveryItemDto> getBulkDeliveryItems(DeliveryFilterDto filter) throws CommonException;

    byte[] downloadBulkDeliveryItemsExcel(DeliveryFilterDto filter) throws CommonException;
}
