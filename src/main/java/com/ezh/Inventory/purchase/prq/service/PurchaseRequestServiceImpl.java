package com.ezh.Inventory.purchase.prq.service;

import com.ezh.Inventory.items.repository.ItemRepository;
import com.ezh.Inventory.purchase.prq.dto.PurchaseRequestDto;
import com.ezh.Inventory.purchase.prq.dto.PurchaseRequestFilter;
import com.ezh.Inventory.purchase.prq.dto.PurchaseRequestItemDto;
import com.ezh.Inventory.purchase.prq.entity.PrqSource;
import com.ezh.Inventory.purchase.prq.entity.PrqStatus;
import com.ezh.Inventory.purchase.prq.entity.PurchaseRequest;
import com.ezh.Inventory.purchase.prq.entity.PurchaseRequestItem;
import com.ezh.Inventory.purchase.prq.repository.PurchaseRequestRepository;
import com.ezh.Inventory.security.UserContext;
import com.ezh.Inventory.utils.UserContextUtil;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.DocPrefix;
import com.ezh.Inventory.utils.common.DocumentNumberUtil;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseRequestServiceImpl implements PurchaseRequestService {

    private final PurchaseRequestRepository prqRepository;
    private final AuthServiceClient authServiceClient;
    private final ItemRepository itemRepository;
    private final UserContext userContext;

    @Override
    @Transactional
    public CommonResponse<?> createPrq(PurchaseRequestDto dto) throws CommonException {
        Long tenantId = userContext.getTenantId();

        PrqStatus status =
                PrqStatus.DRAFT.equals(dto.getStatus())
                        ? PrqStatus.DRAFT
                        : PrqStatus.PENDING;

        // 1. Map DTO to Entity Header
        PurchaseRequest prq = PurchaseRequest.builder()
                .tenantId(tenantId)
                .vendorId(dto.getVendorId())
                .warehouseId(dto.getWarehouseId())
                .requestedBy(dto.getRequestedBy())
                .department(dto.getDepartment())
                .prqNumber(DocumentNumberUtil.generate(DocPrefix.PRQ))
                .status(status)
                .source(PrqSource.SEM_TEAM)
                .createdBy(userContext.getUserId())
                .notes(dto.getNotes())
                .build();

        // 2. Map Items and calculate total
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (var itemDto : dto.getItems()) {
            BigDecimal lineTotal = itemDto.getEstimatedUnitPrice()
                    .multiply(BigDecimal.valueOf(itemDto.getRequestedQty()));
            totalAmount = totalAmount.add(lineTotal);

            PurchaseRequestItem item = PurchaseRequestItem.builder()
                    .itemId(itemDto.getItemId())
                    .requestedQty(itemDto.getRequestedQty())
                    .estimatedUnitPrice(itemDto.getEstimatedUnitPrice())
                    .lineTotal(lineTotal)
                    .build();

            // This helper method sets the parent-child relationship
            prq.addItem(item);
        }

        prq.setTotalEstimatedAmount(totalAmount);

        // 3. Save only the parent (Cascading saves the children)
        prqRepository.save(prq);

        return CommonResponse.builder()
                .id(prq.getId().toString())
                .message("Purchase Request created successfully")
                .build();
    }


    @Override
    @Transactional
    public CommonResponse<?> updatePrq(Long prqId, PurchaseRequestDto dto) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        // 1. Fetch existing PRQ
        PurchaseRequest prq = prqRepository.findByIdAndTenantId(prqId, tenantId)
                .orElseThrow(() -> new CommonException("Purchase Request not found", HttpStatus.NOT_FOUND));

        // 2. Validation: Only allow updates if still PENDING
        PrqStatus status = prq.getStatus();

        if (!(status == PrqStatus.DRAFT || status == PrqStatus.PENDING)) {
            throw new CommonException(
                    "Only DRAFT or PENDING requests can be edited",
                    HttpStatus.BAD_REQUEST
            );
        }

        // 3. Update Header Fields
        prq.setRequestedBy(dto.getRequestedBy());
        prq.setDepartment(dto.getDepartment());
        prq.setNotes(dto.getNotes());
        prq.setStatus(PrqStatus.PENDING);

        // 4. Update Items (Clear and Re-add)
        // Because orphanRemoval = true is set on the entity,
        // clearing the list will delete the old records from the DB.
        prq.getItems().clear();

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (var itemDto : dto.getItems()) {
            BigDecimal lineTotal = itemDto.getEstimatedUnitPrice()
                    .multiply(BigDecimal.valueOf(itemDto.getRequestedQty()));
            totalAmount = totalAmount.add(lineTotal);

            PurchaseRequestItem newItem = PurchaseRequestItem.builder()
                    .itemId(itemDto.getItemId())
                    .requestedQty(itemDto.getRequestedQty())
                    .estimatedUnitPrice(itemDto.getEstimatedUnitPrice())
                    .lineTotal(lineTotal)
                    .build();

            prq.addItem(newItem); // Links back to 'prq' parent
        }

        prq.setTotalEstimatedAmount(totalAmount);

        // 5. Save Parent (Cascades updates/deletes to items)
        prqRepository.save(prq);

        return CommonResponse.builder()
                .id(prq.getId().toString())
                .message("Purchase Request updated successfully")
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseRequestDto> getAllPrqs(Integer page, Integer size, PurchaseRequestFilter filter) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Long vendorId = null;
        if (Objects.equals(userContext.getUserType(), "VENDOR")) {
            vendorId = userContext.getUserId();
        }else {
            vendorId = filter.getVendorId();
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        // Assuming you add a custom query in Repository, or use standard findAll
        Page<PurchaseRequest> prqPage = prqRepository.findAllPurchaseRequests(
                tenantId,
                filter.getId(),
                vendorId,
                filter.getStatuses(),
                filter.getSearchQuery(),
                filter.getStartDateTime(),
                filter.getEndDateTime(),
                pageable
        );

        Set<Long> vendorIds = prqPage.stream().map(PurchaseRequest::getVendorId).collect(Collectors.toSet());

        Map<Long, UserMiniDto> vendorDetails = authServiceClient.getBulkUserDetails(vendorIds.stream().toList());

        return prqPage.map(prq -> mapToDto(prq, vendorDetails));
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseRequestDto getPrqById(Long prqId) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        PurchaseRequest purchaseRequest = prqRepository.findByIdAndTenantId(prqId, tenantId)
                .orElseThrow(() -> new CommonException("PRQ not found", HttpStatus.NOT_FOUND));

        Map<Long, UserMiniDto> vendorDetails = authServiceClient.getBulkUserDetails(
                List.of(purchaseRequest.getVendorId())
        );

        return mapToDto(purchaseRequest, vendorDetails);
    }

    @Override
    @Transactional
    public CommonResponse<?> updateStatus(Long prqId, PrqStatus status) throws CommonException {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        PurchaseRequest prq = prqRepository.findByIdAndTenantId(prqId, tenantId)
                .orElseThrow(() -> new CommonException("PRQ not found", HttpStatus.NOT_FOUND));

        prq.setStatus(status);
        prqRepository.save(prq);

        return CommonResponse.builder()
                .id(prqId.toString())
                .message("Status updated to " + status)
                .build();
    }


    private PurchaseRequestDto mapToDto(PurchaseRequest prq, Map<Long, UserMiniDto> vendorDetails) {
        Set<Long> ids = prq.getItems().stream().map(PurchaseRequestItem::getItemId).collect(Collectors.toSet());

        Map<Long, String> items = itemRepository
                .findByIdIn(ids.stream().toList())
                .stream()
                .map(item -> Map.entry(item.getId(), item.getName()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return PurchaseRequestDto.builder()
                .id(prq.getId())
                .vendorId(prq.getVendorId())
                .vendorDetails(vendorDetails.get(prq.getVendorId()))
                .warehouseId(prq.getWarehouseId())
                .requestedBy(prq.getRequestedBy())
                .department(prq.getDepartment())
                .prqNumber(prq.getPrqNumber())
                .status(prq.getStatus())
                .source(prq.getSource())
                .totalEstimatedAmount(prq.getTotalEstimatedAmount())
                .notes(prq.getNotes())
                .createdAt(prq.getCreatedAt())
                .items(prq.getItems().stream().map(item ->
                        PurchaseRequestItemDto.builder()
                                .Id(item.getId())
                                .itemId(item.getItemId())
                                .itemName(items.get(item.getItemId()) != null ? items.get(item.getItemId()) : "Unknown Item")
                                .requestedQty(item.getRequestedQty())
                                .estimatedUnitPrice(item.getEstimatedUnitPrice())
                                .lineTotal(item.getLineTotal())
                                .build()
                ).collect(Collectors.toList()))
                .build();
    }
}