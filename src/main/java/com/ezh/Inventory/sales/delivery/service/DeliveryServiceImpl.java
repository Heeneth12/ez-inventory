package com.ezh.Inventory.sales.delivery.service;

import com.ezh.Inventory.sales.delivery.dto.*;
import com.ezh.Inventory.sales.delivery.entity.*;
import com.ezh.Inventory.sales.delivery.repository.DeliveryItemRepository;
import com.ezh.Inventory.sales.delivery.repository.DeliveryRepository;
import com.ezh.Inventory.sales.delivery.repository.RouteRepository;
import com.ezh.Inventory.sales.invoice.dto.InvoiceCreateDto;
import com.ezh.Inventory.sales.invoice.dto.InvoiceDto;
import com.ezh.Inventory.sales.invoice.dto.InvoiceItemDto;
import com.ezh.Inventory.sales.invoice.entity.Invoice;
import com.ezh.Inventory.sales.invoice.entity.InvoiceDeliveryStatus;
import com.ezh.Inventory.sales.invoice.entity.InvoiceStatus;
import com.ezh.Inventory.sales.invoice.repository.InvoiceRepository;
import com.ezh.Inventory.utils.UserContextUtil;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.DocPrefix;
import com.ezh.Inventory.utils.common.DocumentNumberUtil;
import com.ezh.Inventory.utils.common.Status;
import com.ezh.Inventory.utils.common.client.AuthServiceClient;
import com.ezh.Inventory.utils.common.dto.UserMiniDto;
import com.ezh.Inventory.utils.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryItemRepository deliveryItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final RouteRepository routeRepository;
    private final AuthServiceClient authServiceClient;


    @Override
    @Transactional
    public void createDeliveryForInvoice(Invoice invoice, InvoiceCreateDto dto) {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        // 1. Determine Status & Dates based on Type
        ShipmentType type = dto.getDeliveryType(); // Defaults to COURIER if null in DTO?
        if (type == null) type = ShipmentType.IN_HOUSE_DELIVERY; // Default

        ShipmentStatus initialStatus;
        Date scheduledDate = null;
        Date shippedDate = null;
        Date deliveredDate = null;

        if (type == ShipmentType.CUSTOMER_PICKUP) {
            // Instant Handover
            initialStatus = ShipmentStatus.DELIVERED;
            shippedDate = new Date();
            deliveredDate = new Date();
            scheduledDate = new Date();

            //update invoice delivered
            invoice.setStatus(InvoiceStatus.ISSUED);
            invoice.setDeliveryStatus(InvoiceDeliveryStatus.DELIVERED);
            invoiceRepository.save(invoice);

        } else {
            // Queue for Dispatch
            initialStatus = ShipmentStatus.SCHEDULED; // Goes to "Todo List"
            // If user provided a specific date, use it, else default to today
            scheduledDate = dto.getScheduledDate();
            //deliveredDate = new Date();
            //scheduledDate = new Date();

        }

        // 2. Create Header
        Delivery delivery = Delivery.builder()
                .tenantId(tenantId)
                .deliveryNumber(DocumentNumberUtil.generate(DocPrefix.DEL))
                .invoice(invoice)
                .customerId(invoice.getCustomerId())
                .type(type)
                .status(initialStatus)
                .scheduledDate(scheduledDate)
                .shippedDate(shippedDate)
                .deliveredDate(deliveredDate)
                .deliveryAddress(dto.getShippingAddress()) // Important for courier
//                .contactPerson(invoice.getCustomer().getName())
//                .contactPhone(invoice.getCustomer().getPhone())
                .build();

        deliveryRepository.save(delivery);

        // 3. Create Delivery Items (Copy from Invoice Items)
        // This snapshots exactly what is in THIS box.
        List<DeliveryItem> items = invoice.getItems().stream().map(invItem -> DeliveryItem.builder()
                .delivery(delivery)
                .itemId(invItem.getItemId())
                .itemName(invItem.getItemName())
                .invoiceItemId(invItem.getId())
                .batchNumber(invItem.getBatchNumber())
                .quantity(invItem.getQuantity())
                .build()
        ).collect(Collectors.toList());

        deliveryItemRepository.saveAll(items);
        log.info("Delivery Record Created: {} | Status: {}", delivery.getDeliveryNumber(), initialStatus);
    }

    @Override
    @Transactional
    public CommonResponse markAsShipped(Long deliveryId, String trackingNumber) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        if (delivery.getStatus() == ShipmentStatus.DELIVERED) {
            throw new CommonException("Already Delivered", HttpStatus.NOT_FOUND);
        }

        delivery.setStatus(ShipmentStatus.SHIPPED);
        delivery.setShippedDate(new Date());
        // delivery.setTrackingNumber(trackingNumber); // Add this field to Entity if needed
        deliveryRepository.save(delivery);

        return CommonResponse.builder().message("Order Shipped").build();
    }

    @Override
    @Transactional
    public CommonResponse<?> markAsDelivered(Long deliveryId) {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        Invoice invoice = invoiceRepository.findByIdAndTenantId(delivery.getInvoice().getId(), tenantId)
                        .orElseThrow(() -> new CommonException("Invoice not found", HttpStatus.NOT_FOUND));

        invoice.setDeliveryStatus(InvoiceDeliveryStatus.DELIVERED);
        invoiceRepository.save(invoice);

        delivery.setStatus(ShipmentStatus.DELIVERED);
        delivery.setDeliveredDate(new Date());
        deliveryRepository.save(delivery);

        return CommonResponse.builder().message("Order Delivered Successfully").build();
    }

    @Override
    @Transactional
    public CommonResponse<?> createDeliveryFromInvoice(Long invoiceId) throws CommonException {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new CommonException("Invoice not found", HttpStatus.NOT_FOUND));

        // Validate invoice is not cancelled
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new CommonException("Cannot create delivery for cancelled invoice", HttpStatus.BAD_REQUEST);
        }


        return CommonResponse
                .builder()
                .message("")
                .build();

    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryDto getDeliveryDetail(Long deliveryId) throws CommonException {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new CommonException("Delivery not found with ID: " + deliveryId, HttpStatus.NOT_FOUND));
        Map<Long, UserMiniDto> customerMap = authServiceClient.getBulkUserDetails(List.of(delivery.getCustomerId()));

        return mapToDto(delivery, customerMap, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryDto> getAllDeliveries(int page, int size) throws CommonException {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        Page<Delivery> deliveryPage = deliveryRepository.findAll(pageable);

        List<Long> customerIds = deliveryPage.getContent().stream()
                .map(Delivery::getCustomerId)
                .distinct()
                .toList();

        Map<Long, UserMiniDto> customerMap = new HashMap<>();
        if (!customerIds.isEmpty()) {
            customerMap = authServiceClient.getBulkUserDetails(customerIds);
        }

            final Map<Long, UserMiniDto> finalMap = customerMap;
        return deliveryPage.map(d -> mapToDto(d, finalMap, true));
    }

    @Override
    @Transactional
    public CommonResponse<?> updateDeliveryStatus(DeliveryFilterDto dto) throws CommonException {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Delivery delivery = deliveryRepository
                .findByIdAndTenantId(dto.getId(), tenantId)
                .orElseThrow(() -> new CommonException("Delivery not found", HttpStatus.NOT_FOUND));

        Invoice invoice = invoiceRepository.findById(delivery.getInvoice().getId())
                .orElseThrow(() -> new CommonException("", HttpStatus.NOT_FOUND));

        ShipmentStatus currentStatus = delivery.getStatus();
        ShipmentStatus newStatus = dto.getStatus();

        //Same status → no update
        if (currentStatus == newStatus) {
            return CommonResponse.builder()
                    .message("Status already updated")
                    .build();
        }

        //Validate transition
        if (!isValidTransition(currentStatus, newStatus)) {
            throw new CommonException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus,
                    HttpStatus.BAD_REQUEST
            );
        }

        if(newStatus.equals(ShipmentStatus.SHIPPED)){
            delivery.setShippedDate(new Date());
        }

        if(newStatus.equals(ShipmentStatus.DELIVERED)){
            delivery.setDeliveredDate(new Date());
            invoice.setDeliveryStatus(InvoiceDeliveryStatus.DELIVERED);
        }

        delivery.setStatus(newStatus);
        deliveryRepository.save(delivery);

        return CommonResponse.builder()
                .message("Delivery status updated successfully")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryDto> searchDeliveryDetails(DeliveryFilterDto filter) throws CommonException {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        List<Delivery> deliveries = deliveryRepository.searchDeliveries(
                tenantId,
                filter.getId(),
                filter.getDeliveryNumber(),
                filter.getInvoiceId(),
                filter.getCustomerId(),
                filter.getType(),
                filter.getStatus(),
                filter.getScheduledDate(),
                filter.getShippedDate(),
                filter.getDeliveredDate()
        );
        final Map<Long, UserMiniDto> finalMap = new HashMap<>();
        return deliveries.stream().map(dev ->  mapToDto(dev, finalMap, false)).toList();
    }


    @Override
    @Transactional
    public CommonResponse<?> rescheduleDelivery(Long deliveryId, Date newDate, String reason) {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Delivery delivery = deliveryRepository.findByIdAndTenantId(deliveryId, tenantId)
                .orElseThrow(() -> new CommonException("Delivery not found", HttpStatus.NOT_FOUND));

        // Update to Scheduled and set the new date
        delivery.setStatus(ShipmentStatus.SCHEDULED);
        delivery.setScheduledDate(newDate);
        delivery.setRemarks(delivery.getRemarks() + " | Rescheduled: " + reason);

        deliveryRepository.save(delivery);

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .message("Delivery rescheduled to " + newDate)
                .build();
    }

    @Override
    @Transactional
    public CommonResponse<?> cancelDelivery(Long deliveryId, String reason) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Delivery delivery = deliveryRepository.findByIdAndTenantId(deliveryId, tenantId)
                .orElseThrow(() -> new CommonException("Delivery not found", HttpStatus.NOT_FOUND));

        delivery.setStatus(ShipmentStatus.CANCELLED);
        delivery.setRemarks(delivery.getRemarks() + " | Cancelled: " + reason);

        // Update parent invoice delivery status back to PENDING so it can be re-processed later
        Invoice invoice = delivery.getInvoice();
        invoice.setDeliveryStatus(InvoiceDeliveryStatus.CANCELLED);

        deliveryRepository.save(delivery);
        invoiceRepository.save(invoice);

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .message("Delivery cancelled successfully")
                .build();
    }


    @Override
    @Transactional
    public CommonResponse<?> createRoute(RouteCreateDto dto) throws CommonException{
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        // 1. Create Route Header
        Route route = Route.builder()
                .tenantId(tenantId)
                .routeNumber("RT-" + System.currentTimeMillis())
                .areaName(dto.getAreaName()) // Can be null as per your request
                .vehicleNumber(dto.getVehicleNumber())
                .employeeId(dto.getEmployeeId())
                .status(RouteStatus.CREATED)
                .build();

        route = routeRepository.save(route);

        // 2. Batch Update Deliveries
        if (dto.getDeliveryIds() != null && !dto.getDeliveryIds().isEmpty()) {
            List<Delivery> deliveries = deliveryRepository.findAllById(dto.getDeliveryIds());
            for (Delivery delivery : deliveries) {
                // Ensure we only batch "SCHEDULED" deliveries
                if (delivery.getStatus() == ShipmentStatus.SCHEDULED) {
                    delivery.setRoute(route);

                }
            }
            deliveryRepository.saveAll(deliveries);
        }

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .message("Route Batch Created Successfully")
                .id(route.getId().toString())
                .build();
    }

    @Override
    @Transactional
    public CommonResponse<?> startRoute(Long routeId) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        Route route = routeRepository.findByIdAndTenantId(routeId, tenantId)
                .orElseThrow(() -> new CommonException("Route not found", HttpStatus.NOT_FOUND));

        route.setStatus(RouteStatus.IN_TRANSIT);
        route.setStartDate(new Date());

        // Batch Update all deliveries in this route to SHIPPED
        List<Delivery> deliveries = route.getDeliveries();
        deliveries.forEach(del -> {
            del.setStatus(ShipmentStatus.SHIPPED);
            del.setShippedDate(new Date());
        });

        deliveryRepository.saveAll(deliveries);
        routeRepository.save(route);

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .message("Route started. All deliveries marked as SHIPPED.")
                .build();
    }

    @Override
    @Transactional
    public CommonResponse<?> completeRoute(Long routeId) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        Route route = routeRepository.findByIdAndTenantId(routeId, tenantId)
                .orElseThrow(() -> new CommonException("Route not found", HttpStatus.NOT_FOUND));

        // Check if all deliveries are already 'DELIVERED'
        boolean allDone = route.getDeliveries().stream()
                .allMatch(d -> d.getStatus() == ShipmentStatus.DELIVERED);

        if (!allDone) {
            throw new CommonException("Cannot complete route: Some deliveries are still pending", HttpStatus.BAD_REQUEST);
        }

        route.setStatus(RouteStatus.COMPLETED);
        routeRepository.save(route);

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .message("Route manifest completed.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RouteDto getRouteDetail(Long routeId) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        // 1. Fetch route with all delivery stops joined
        Route route = routeRepository.findByIdWithDeliveries(routeId, tenantId)
                .orElseThrow(() -> new CommonException("Route manifest not found", HttpStatus.NOT_FOUND));

        // 2. Map to DTO including the list of delivery stops
        return mapToRouteDto(route);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RouteDto> getAllRoutes(int page, int size) throws CommonException {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        Page<Route> routePage = routeRepository.findByTenantId(tenantId, pageable);

        return routePage.map(this::mapToRouteDto);
    }


    @Override
    @Transactional(readOnly = true)
    public RouteSummaryDto getRouteSummary() throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        long pending = deliveryRepository.countPendingDeliveries(tenantId);
        long delivered = deliveryRepository.countByStatus(tenantId, ShipmentStatus.DELIVERED);
        long cancelled = deliveryRepository.countByStatus(tenantId, ShipmentStatus.CANCELLED);
        long totalRoutes = deliveryRepository.countTotalRoutes(tenantId);

        // Calculate Efficiency (Completed / Total)
        long totalItems = pending + delivered + cancelled;
        String efficiency = totalItems > 0 ? (delivered * 100 / totalItems) + "%" : "0%";

        return RouteSummaryDto.builder()
                .totalRoutes(totalRoutes)
                .pendingDeliveries(pending)
                .completedDeliveries(delivered)
                .cancelledDeliveries(cancelled)
                .routeEfficiency(efficiency)
                .build();
    }

    /**
     * Helper method to map Route Entity to RouteDto
     */
    private RouteDto mapToRouteDto(Route route) {
        return RouteDto.builder()
                .id(route.getId())
                .routeNumber(route.getRouteNumber())
                .areaName(route.getAreaName())
                .employeeId(route.getEmployeeId())
                .employeeName("Unassigned")
                .vehicleNumber(route.getVehicleNumber())
                .status(route.getStatus())
                .startDate(route.getStartDate())
                // Map the list of deliveries associated with this route batch
                .deliveries(route.getDeliveries().stream()
                        .map(this::mapToDeliveryStopDto)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Reusing your existing Delivery mapping logic
     */
    private DeliveryDto mapToDeliveryStopDto(Delivery delivery) {
        return DeliveryDto.builder()
                .id(delivery.getId())
                .deliveryNumber(delivery.getDeliveryNumber())
                .status(delivery.getStatus())
                .customerId(delivery.getCustomerId())
                .customerName("Test")
                .deliveryAddress(delivery.getDeliveryAddress())
                .contactPerson(delivery.getContactPerson())
                .contactPhone(delivery.getContactPhone())
                .shippedDate(delivery.getShippedDate())
                .deliveredDate(delivery.getDeliveredDate())
                .build();
    }
    private boolean isValidTransition(ShipmentStatus current, ShipmentStatus next) {

        switch (current) {
            case PENDING:
                return next == ShipmentStatus.SCHEDULED || next == ShipmentStatus.CANCELLED;

            case SCHEDULED:
                return next == ShipmentStatus.SHIPPED || next == ShipmentStatus.CANCELLED;

            case SHIPPED:
                return next == ShipmentStatus.DELIVERED;

            case DELIVERED:
            case CANCELLED:
                return false; // final states
        }
        return false;
    }


    private DeliveryDto mapToDto(Delivery delivery, Map<Long, UserMiniDto> customerMap, boolean includeContact) {

        UserMiniDto contactMini = null;
        String customerName = null;

        if (includeContact && customerMap != null) {
            UserMiniDto userDetail = customerMap.getOrDefault(delivery.getCustomerId(), new UserMiniDto());

            contactMini = UserMiniDto.builder()
                    .id(userDetail.getId())
                    .userType(userDetail.getUserType())
                    .userUuid(userDetail.getUserUuid())
                    .name(userDetail.getName())
                    .email(userDetail.getEmail())
                    .phone(userDetail.getPhone())
                    .build();

            customerName = userDetail.getName();
        }

        return DeliveryDto.builder()
                .id(delivery.getId())
                .tenantId(delivery.getTenantId())
                .deliveryNumber(delivery.getDeliveryNumber())
                .type(delivery.getType())
                .status(delivery.getStatus())
                .invoice(mapToDto(delivery.getInvoice()))
                .customerId(delivery.getCustomerId())
                .contactMini(contactMini)
                .customerName(customerName)
                .scheduledDate(delivery.getScheduledDate())
                .shippedDate(delivery.getShippedDate())
                .deliveredDate(delivery.getDeliveredDate())
                .deliveryAddress(delivery.getDeliveryAddress())
                .contactPerson(delivery.getContactPerson())
                .contactPhone(delivery.getContactPhone())
                .remarks(delivery.getRemarks())
                .build();
    }


    public InvoiceDto mapToDto(Invoice invoice) {
        List<InvoiceItemDto> itemDtos = invoice.getItems().stream()
                .map(item -> InvoiceItemDto.builder()
                        .id(item.getId())
                        .itemId(item.getItemId())
                        .itemName(item.getItemName())
                        .sku(item.getSku())
                        .batchNumber(item.getBatchNumber())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getLineTotal())
                        .build())
                .collect(Collectors.toList());

        return InvoiceDto.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .salesOrderId(invoice.getSalesOrder() != null ? invoice.getSalesOrder().getId() : null)
                .customerId(invoice.getCustomerId())
                .invoiceDate(invoice.getInvoiceDate())
                .status(invoice.getStatus())
                .subTotal(invoice.getSubTotal())
                .grandTotal(invoice.getGrandTotal())
                .balance(invoice.getBalance())
                .items(itemDtos)
                .build();
    }

}
