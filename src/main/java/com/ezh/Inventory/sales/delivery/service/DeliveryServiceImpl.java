package com.ezh.Inventory.sales.delivery.service;

import com.ezh.Inventory.contacts.dto.ContactMiniDto;
import com.ezh.Inventory.sales.delivery.dto.DeliveryDto;
import com.ezh.Inventory.sales.delivery.dto.DeliveryFilterDto;
import com.ezh.Inventory.sales.delivery.entity.Delivery;
import com.ezh.Inventory.sales.delivery.entity.DeliveryItem;
import com.ezh.Inventory.sales.delivery.entity.ShipmentStatus;
import com.ezh.Inventory.sales.delivery.entity.ShipmentType;
import com.ezh.Inventory.sales.delivery.repository.DeliveryItemRepository;
import com.ezh.Inventory.sales.delivery.repository.DeliveryRepository;
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
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryItemRepository deliveryItemRepository;
    private final InvoiceRepository invoiceRepository;


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
                .customer(invoice.getCustomer())
                .type(type)
                .status(initialStatus)
                .scheduledDate(scheduledDate)
                .shippedDate(shippedDate)
                .deliveredDate(deliveredDate)
                .deliveryAddress(dto.getShippingAddress()) // Important for courier
                .contactPerson(invoice.getCustomer().getName())
                .contactPhone(invoice.getCustomer().getPhone())
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
    @Transactional
    public CommonResponse<?> updateDeliveryStatus(Long deliveryId, ShipmentStatus newStatus) throws CommonException {


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

        return mapToDto(delivery);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryDto> getAllDeliveries(int page, int size) throws CommonException {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        Page<Delivery> deliveryPage = deliveryRepository.findAll(pageable);

        return deliveryPage.map(this::mapToDto);
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

        return deliveries.stream().map(this::mapToDto).toList();

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


    private DeliveryDto mapToDto(Delivery delivery) {

        return DeliveryDto.builder()
                .id(delivery.getId())
                .tenantId(delivery.getTenantId())
                .deliveryNumber(delivery.getDeliveryNumber())
                .type(delivery.getType())
                .status(delivery.getStatus())
                .invoice(mapToDto(delivery.getInvoice()))
                .contactMini(new ContactMiniDto(delivery.getCustomer()))
                .customerId(delivery.getCustomer() != null ? delivery.getCustomer().getId() : null)
                .customerName(delivery.getCustomer() != null ? delivery.getCustomer().getName() : null)
                .scheduledDate(delivery.getScheduledDate())
                .shippedDate(delivery.getShippedDate())
                .deliveredDate(delivery.getDeliveredDate())
                .deliveryAddress(delivery.getDeliveryAddress())
                .contactPerson(delivery.getContactPerson())
                .contactPhone(delivery.getContactPhone())
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
                .customerName(invoice.getCustomer().getName())
                .invoiceDate(invoice.getInvoiceDate())
                .status(invoice.getStatus())
                .subTotal(invoice.getSubTotal())
                .grandTotal(invoice.getGrandTotal())
                .balance(invoice.getBalance())
                .items(itemDtos)
                .build();
    }

}
