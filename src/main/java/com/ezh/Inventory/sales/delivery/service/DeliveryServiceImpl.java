package com.ezh.Inventory.sales.delivery.service;

import com.ezh.Inventory.sales.delivery.dto.*;
import com.ezh.Inventory.sales.delivery.entity.*;
import com.ezh.Inventory.sales.delivery.repository.DeliveryItemRepository;
import com.ezh.Inventory.sales.delivery.repository.DeliveryRepository;
import com.ezh.Inventory.sales.delivery.repository.RouteRepository;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

import com.ezh.Inventory.common.storage.dto.FileRecordResponse;
import com.ezh.Inventory.common.storage.dto.FileUploadRequest;
import com.ezh.Inventory.common.storage.entity.FileReferenceType;
import com.ezh.Inventory.common.storage.entity.FileType;
import com.ezh.Inventory.common.storage.service.FileStorageService;
import com.ezh.Inventory.items.entity.Item;
import com.ezh.Inventory.items.repository.ItemRepository;
import com.ezh.Inventory.stock.dto.StockUpdateDto;
import com.ezh.Inventory.stock.entity.MovementType;
import com.ezh.Inventory.stock.entity.ReferenceType;
import com.ezh.Inventory.stock.entity.StockBatch;
import com.ezh.Inventory.stock.repository.StockBatchRepository;
import com.ezh.Inventory.stock.service.StockService;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryItemRepository deliveryItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final RouteRepository routeRepository;
    private final AuthServiceClient authServiceClient;
    private final ItemRepository itemRepository;
    private final StockBatchRepository stockBatchRepository;
    private final StockService stockService;
    private final FileStorageService fileStorageService;


    @Override
    @Transactional
    public void createDeliveryForInvoice(Invoice invoice, InvoiceDto dto) {
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

        } else if (dto.getScheduledDate() != null) {
            // Queue for Dispatch
            initialStatus = ShipmentStatus.SCHEDULED; // Goes to "Todo List"
            // If user provided a specific date, use it, else default to today
            scheduledDate = dto.getScheduledDate();
            //deliveredDate = new Date();
            //scheduledDate = new Date();
        } else {
            initialStatus = ShipmentStatus.PENDING;
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
    @Transactional(readOnly = true)
    public DeliveryDto getDeliveryDetail(Long deliveryId) throws CommonException {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new CommonException("Delivery not found with ID: " + deliveryId, HttpStatus.NOT_FOUND));
        Map<Long, UserMiniDto> customerMap = authServiceClient.getBulkUserDetails(List.of(delivery.getCustomerId()), true);

        return mapToDto(delivery, customerMap, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryDto> getAllDeliveries(int page, int size, DeliveryFilterDto filter) throws CommonException {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        // Call the custom query with all filter parameters
        Page<Delivery> deliveryPage = deliveryRepository.findDeliveriesByFilter(
                tenantId,
                filter.getDeliveryId(),
                filter.getInvoiceId(),
                filter.getCustomerId(),
                filter.getShipmentTypes(),
                filter.getShipmentStatuses(),
                filter.getSearchQuery(),
                filter.getStartDateTime(),
                filter.getEndDateTime(),
                pageable
        );

        // Bulk fetch customer details for mapping
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
    public CommonResponse<?> updateDeliveryStatus(Long id, DeliveryStatusUpdateRequest request, MultipartFile file) throws CommonException {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Delivery delivery = deliveryRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new CommonException("Delivery not found", HttpStatus.NOT_FOUND));

        Invoice invoice = invoiceRepository.findById(delivery.getInvoice().getId())
                .orElseThrow(() -> new CommonException("Invoice not found", HttpStatus.NOT_FOUND));

        ShipmentStatus status = request.getStatus();
        String reason = request.getReason() != null ? request.getReason() : "";
        ShipmentStatus currentStatus = delivery.getStatus();

        if (currentStatus == status) {
            return CommonResponse.builder().message("Status already set to " + status).build();
        }

        if (!isValidTransition(currentStatus, status)) {
            throw new CommonException(
                    "Invalid status transition from " + currentStatus + " to " + status,
                    HttpStatus.BAD_REQUEST
            );
        }

        switch (status) {
            case SCHEDULED -> {
                Date scheduledDate = request.getScheduledDate() != null ? request.getScheduledDate() : delivery.getScheduledDate();
                delivery.setScheduledDate(scheduledDate);
                String existingRemarks = delivery.getRemarks() != null ? delivery.getRemarks() : "";
                if (!reason.isBlank()) {
                    delivery.setRemarks(existingRemarks + " | Rescheduled: " + reason);
                }
            }
            case SHIPPED -> delivery.setShippedDate(new Date());
            case DELIVERED -> {
                if (file != null && !file.isEmpty()) {
                    FileUploadRequest uploadRequest = new FileUploadRequest();
                    uploadRequest.setReferenceId(delivery.getId().toString());
                    uploadRequest.setReferenceType(FileReferenceType.DELIVERY);
                    uploadRequest.setFileType(FileType.DELIVERY_PROOF_PHOTO);
                    uploadRequest.setTenantId(tenantId.toString());
                    uploadRequest.setDescription("Proof of delivery for " + delivery.getDeliveryNumber());
                    FileRecordResponse uploaded = fileStorageService.uploadFile(file, uploadRequest);
                    delivery.setAttachmentUuid(uploaded.getUuid());
                }
                delivery.setDeliveredDate(new Date());
                invoice.setDeliveryStatus(InvoiceDeliveryStatus.DELIVERED);
                invoice.setStatus(InvoiceStatus.ISSUED);
            }
            case CANCELLED -> {
                if (delivery.getStatus() == ShipmentStatus.DELIVERED) {
                    throw new CommonException("Cannot cancel a delivery that is already delivered", HttpStatus.BAD_REQUEST);
                }
                for (DeliveryItem item : delivery.getItems()) {
                    if (item.getBatchNumber() == null || item.getBatchNumber().isBlank()) {
                        log.warn("Delivery item {} has no batch number — skipping stock restoration", item.getItemId());
                        continue;
                    }
                    StockUpdateDto stockDto = StockUpdateDto.builder()
                            .itemId(item.getItemId())
                            .warehouseId(invoice.getWarehouseId())
                            .quantity(item.getQuantity())
                            .transactionType(MovementType.IN)
                            .referenceType(ReferenceType.CANCEL_DELIVERY)
                            .referenceId(delivery.getId())
                            .batchNumber(item.getBatchNumber())
                            .remarks("Stock restored — Delivery " + delivery.getDeliveryNumber() + " cancelled: " + reason)
                            .build();
                    stockService.updateStock(stockDto);
                }
                String existingRemarks = delivery.getRemarks() != null ? delivery.getRemarks() : "";
                delivery.setRemarks(existingRemarks + " | Cancelled: " + reason);
                invoice.setDeliveryStatus(InvoiceDeliveryStatus.CANCEL_DELIVERY);
                log.info("Delivery {} cancelled. Stock restored for {} items.", delivery.getDeliveryNumber(), delivery.getItems().size());
            }
            default -> throw new CommonException("Unsupported status: " + status, HttpStatus.BAD_REQUEST);
        }

        delivery.setStatus(status);
        deliveryRepository.save(delivery);
        invoiceRepository.save(invoice);

        return CommonResponse.builder()
                .message("Delivery status updated to " + status)
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
                filter.getShipmentTypes(),
                filter.getShipmentStatuses(),
                filter.getStartDateTime(),
                filter.getEndDateTime()
        );

        List<Long> customerIds = deliveries.stream()
                .map(Delivery::getCustomerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, UserMiniDto> customerMap = authServiceClient.getBulkUserDetails(customerIds, true);

        return deliveries.stream().map(dev -> mapToDto(dev, customerMap, true)).toList();
    }


    @Override
    @Transactional
    public CommonResponse<?> createRoute(RouteCreateDto dto) throws CommonException {
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
                return next == ShipmentStatus.SCHEDULED || next == ShipmentStatus.SHIPPED || next == ShipmentStatus.CANCELLED;

            case SCHEDULED:
                return next == ShipmentStatus.SHIPPED || next == ShipmentStatus.PENDING || next == ShipmentStatus.CANCELLED;

            case SHIPPED:
                // DELIVERED = item handed over; CANCELLED = customer not home or refused
                return next == ShipmentStatus.DELIVERED || next == ShipmentStatus.CANCELLED;

            case DELIVERED:
            case CANCELLED:
                return false; // terminal states
        }
        return false;
    }


    private DeliveryDto mapToDto(Delivery delivery, Map<Long, UserMiniDto> customerMap, Boolean includeContact) {

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
                    .userAddresses(userDetail.getUserAddresses())
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
                .attachmentUuid(delivery.getAttachmentUuid())
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
                .grandTotal(invoice.getGrandTotal())
                .balance(invoice.getBalance())
                .items(itemDtos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BulkDeliveryItemDto> getBulkDeliveryItems(DeliveryFilterDto filter) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        List<Delivery> deliveries = deliveryRepository.searchDeliveries(
                tenantId,
                filter.getId(),
                filter.getDeliveryNumber(),
                filter.getInvoiceId(),
                filter.getCustomerId(),
                filter.getShipmentTypes(),
                filter.getShipmentStatuses(),
                filter.getStartDateTime(),
                filter.getEndDateTime()
        );

        if (filter.getInvoiceIds() != null && !filter.getInvoiceIds().isEmpty()) {
            deliveries = deliveries.stream()
                    .filter(d -> d.getInvoice() != null && filter.getInvoiceIds().contains(d.getInvoice().getId()))
                    .collect(Collectors.toList());
        }

        if (filter.getDeliveryIds() != null && !filter.getDeliveryIds().isEmpty()) {
            deliveries = deliveries.stream()
                    .filter(d -> filter.getDeliveryIds().contains(d.getId()))
                    .collect(Collectors.toList());
        }

        Map<String, BulkDeliveryItemDto> itemMap = new HashMap<>();
        for (Delivery delivery : deliveries) {
            for (DeliveryItem item : delivery.getItems()) {
                String key = item.getItemId() + "_" + (item.getBatchNumber() != null ? item.getBatchNumber() : "NO_BATCH");
                itemMap.compute(key, (k, existing) -> {
                    if (existing == null) {
                        return BulkDeliveryItemDto.builder()
                                .itemId(item.getItemId())
                                .itemName(item.getItemName())
                                .batchNumber(item.getBatchNumber())
                                .totalQuantity(item.getQuantity())
                                .build();
                    } else {
                        existing.setTotalQuantity(existing.getTotalQuantity() + item.getQuantity());
                        return existing;
                    }
                });
            }
        }

        List<BulkDeliveryItemDto> resultList = new ArrayList<>(itemMap.values());

        List<Long> itemIds = resultList.stream().map(BulkDeliveryItemDto::getItemId).distinct().collect(Collectors.toList());
        if (!itemIds.isEmpty()) {
            List<Item> items = itemRepository.findAllById(itemIds);
            Map<Long, Item> itemDataMap = items.stream().collect(Collectors.toMap(Item::getId, i -> i));

            for (BulkDeliveryItemDto dto : resultList) {
                Item itemInfo = itemDataMap.get(dto.getItemId());
                if (itemInfo != null) {
                    dto.setItemCode(itemInfo.getItemCode());
                    dto.setSku(itemInfo.getSku());
                    dto.setCategory(itemInfo.getCategory());
                    dto.setBrand(itemInfo.getBrand());
                    dto.setMrp(itemInfo.getMrp());
                    dto.setSellingPrice(itemInfo.getSellingPrice());
                }

                if (dto.getBatchNumber() != null && !dto.getBatchNumber().equals("NO_BATCH")) {
                    Optional<StockBatch> batchOpt = stockBatchRepository.findFirstByItemIdAndBatchNumberAndTenantId(
                            dto.getItemId(), dto.getBatchNumber(), tenantId);
                    batchOpt.ifPresent(stockBatch -> dto.setExpiryDate(stockBatch.getExpiryDate()));
                }
            }
        }

        return resultList;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadBulkDeliveryItemsExcel(DeliveryFilterDto filter) throws CommonException {
        List<BulkDeliveryItemDto> items = getBulkDeliveryItems(filter);
        return com.ezh.Inventory.sales.delivery.utils.DeliveryExportUtils.toBulkItemsExcel(items).readAllBytes();
    }
}
