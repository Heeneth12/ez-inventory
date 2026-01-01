package com.ezh.Inventory.stock.service;


import com.ezh.Inventory.approval.dto.ApprovalCheckContext;
import com.ezh.Inventory.approval.entity.ApprovalResultStatus;
import com.ezh.Inventory.approval.entity.ApprovalStatus;
import com.ezh.Inventory.approval.entity.ApprovalType;
import com.ezh.Inventory.approval.service.ApprovalService;
import com.ezh.Inventory.items.repository.ItemRepository;
import com.ezh.Inventory.stock.dto.*;
import com.ezh.Inventory.stock.entity.*;
import com.ezh.Inventory.stock.repository.StockAdjustmentRepository;
import com.ezh.Inventory.stock.repository.StockRepository;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.DocPrefix;
import com.ezh.Inventory.utils.common.DocumentNumberUtil;
import com.ezh.Inventory.utils.common.Status;
import com.ezh.Inventory.utils.common.events.ApprovalDecisionEvent;
import com.ezh.Inventory.utils.exception.BadRequestException;
import com.ezh.Inventory.utils.exception.CommonException;
import com.ezh.Inventory.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.ezh.Inventory.utils.UserContextUtil.getTenantIdOrThrow;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockAdjustmentServiceImpl implements StockAdjustmentService {

    private final StockRepository stockRepository;
    private final StockService stockService;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final ItemRepository itemRepository;
    private final ApprovalService approvalService;

    @Override
    @Transactional
    public CommonResponse<?> createStockAdjustment(StockAdjustmentCreateDto dto) throws CommonException {
        Long tenantId = getTenantIdOrThrow();

        // 1. Initialize Header (Default to PENDING_APPROVAL or DRAFT initially)
        StockAdjustment adjustment = StockAdjustment.builder()
                .tenantId(tenantId)
                .warehouseId(dto.getWarehouseId())
                .adjustmentNumber(DocumentNumberUtil.generate(DocPrefix.ADJ))
                .adjustmentDate(new Date())
                .status(AdjustmentStatus.DRAFT)
                .reference(dto.getReference())
                .remarks(dto.getRemarks())
                .reasonType(dto.getReasonType())
                .build();

        adjustment = stockAdjustmentRepository.save(adjustment);

        List<StockAdjustmentItem> adjustmentItems = new ArrayList<>();

        BigDecimal totalAdjustmentValue = BigDecimal.ZERO;

        // 2. Process Line Items
        for (StockAdjustmentItemDto itemDto : dto.getItems()) {

            Stock stock = stockRepository
                    .findByItemIdAndWarehouseIdAndTenantId(itemDto.getItemId(), dto.getWarehouseId(), tenantId)
                    .orElse(createNewStock(itemDto.getItemId(), dto.getWarehouseId()));

            int systemQty = stock.getClosingQty();
            int finalCountedQty;
            int difference;

            //Calculate Logic based on REASON TYPE only
            switch (dto.getReasonType()) {
                case DAMAGE:
                case EXPIRED:
                case LOST:
                    // Logic: REMOVE specific quantity
                    // Example: System 100, Input 5 (Damaged) -> Final 95, Diff -5
                    difference = -itemDto.getQuantity();
                    finalCountedQty = systemQty - itemDto.getQuantity();
                    break;

                case FOUND_EXTRA:
                    // Logic: ADD specific quantity
                    // Example: System 100, Input 2 (Found) -> Final 102, Diff +2
                    difference = itemDto.getQuantity();
                    finalCountedQty = systemQty + itemDto.getQuantity();
                    break;

                case AUDIT_CORRECTION:
                    // Logic: ABSOLUTE / REPLACE (Set to actual count)
                    // Example: System 100, Input 90 (Actual Count) -> Final 90, Diff -10
                    finalCountedQty = itemDto.getQuantity();
                    difference = finalCountedQty - systemQty;
                    break;

                default:
                    throw new BadRequestException("Unsupported Adjustment Reason: " + dto.getReasonType());
            }

            if (finalCountedQty < 0) {
                throw new BadRequestException("Adjustment results in negative stock for Item ID: " + itemDto.getItemId());
            }

            // C. Create the Line Item Entity
            StockAdjustmentItem lineItem = StockAdjustmentItem.builder()
                    .stockAdjustment(adjustment)
                    .itemId(itemDto.getItemId())
                    .systemQty(systemQty)
                    .countedQty(finalCountedQty)
                    .differenceQty(difference)
                    .reasonType(dto.getReasonType())
                    .build();

            adjustmentItems.add(lineItem);

            // Accumulate value for approval logic (abs value * cost)
            totalAdjustmentValue = totalAdjustmentValue.add(
                    stock.getAverageCost().multiply(BigDecimal.valueOf(Math.abs(difference)))
            );

            // D. UPDATE ACTUAL INVENTORY
//            if (difference != 0) {
//                MovementType movementType = (difference > 0) ? MovementType.IN : MovementType.OUT;
//
//                StockUpdateDto updateDto = StockUpdateDto.builder()
//                        .itemId(itemDto.getItemId())
//                        .warehouseId(dto.getWarehouseId())
//                        .quantity(Math.abs(difference)) // Use absolute value for update service
//                        .transactionType(movementType)
//                        .referenceType(ReferenceType.ADJUSTMENT)
//                        .referenceId(adjustment.getId())
//                        .unitPrice(stock.getAverageCost())
//                        .batchNumber(itemDto.getBatchNumber())
//                        .build();
//
//                // Assuming your stockService handles the math based on IN/OUT
//                updateStock(updateDto);
//            }
        }

        //

        adjustment.setAdjustmentItems(adjustmentItems);

        // 3. Build Approval Context
        // NOTE: Replaced 'salesOrder.getGrandTotal()' with calculated adjustment value
        ApprovalCheckContext approvalCheckContext = ApprovalCheckContext.builder()
                .type(ApprovalType.STOCK_ADJUSTMENT)
                .amount(totalAdjustmentValue)
                .referenceId(adjustment.getId())
                .referenceCode(adjustment.getAdjustmentNumber())
                .build();
        CommonResponse<?> approvalResponse = approvalService.checkAndInitiateApproval(approvalCheckContext);

        // 4. Decision: Wait or Commit?
        if (approvalResponse.getData() == ApprovalResultStatus.APPROVAL_REQUIRED) {
            // CASE A: Stop here. Wait for approval.
            adjustment.setStatus(AdjustmentStatus.PENDING_APPROVAL);
        } else {
            // CASE B: Auto-approved. Commit stock NOW.
            adjustment.setStatus(AdjustmentStatus.COMPLETED);

            // *** CRITICAL: Perform the Stock Movement Loop Here ***
            applyStockMovements(adjustment, dto.getWarehouseId());
        }

        stockAdjustmentRepository.save(adjustment);

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .message(adjustment.getStatus() == AdjustmentStatus.PENDING_APPROVAL
                        ? "Adjustment submitted for approval"
                        : "Adjustment completed successfully")
                .id(String.valueOf(adjustment.getId()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockAdjustmentListDto> getAllStockAdjustments(StockFilterDto filter, Integer page, Integer size) {
        Long tenantId = getTenantIdOrThrow();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Assuming you have a basic findAll or specification
        Page<StockAdjustment> adjustments = stockAdjustmentRepository.findAllByTenantId(getTenantIdOrThrow(), pageable);

        return adjustments.map(adj -> StockAdjustmentListDto.builder()
                .id(adj.getId())
                .adjustmentNumber(adj.getAdjustmentNumber())
                .adjustmentDate(adj.getAdjustmentDate())
                .status(adj.getStatus())
                .warehouseId(adj.getWarehouseId())
                .reference(adj.getReference())
                .totalItems(adj.getAdjustmentItems().size())
                .build());
    }

    @Override
    @Transactional
    public void approveStockAdjustment(Long adjustmentId) throws CommonException {
        StockAdjustment adjustment = stockAdjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new CommonException("Adjustment not found", HttpStatus.NOT_FOUND));

        if (adjustment.getStatus() != AdjustmentStatus.PENDING_APPROVAL) {
            // Idempotency check: if already completed, do nothing
            if (adjustment.getStatus() == AdjustmentStatus.COMPLETED) return;
            throw new BadRequestException("Adjustment is not pending approval");
        }

        // 1. Execute the Phase 2 Logic (Move Inventory)
        applyStockMovements(adjustment, adjustment.getWarehouseId());

        // 2. Update Status
        adjustment.setStatus(AdjustmentStatus.COMPLETED);
        stockAdjustmentRepository.save(adjustment);
    }

    @Transactional
    public void rejectStockAdjustment(Long adjustmentId) throws CommonException {
        StockAdjustment adjustment = stockAdjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new CommonException("Adjustment not found", HttpStatus.NOT_FOUND));

        if (adjustment.getStatus() != AdjustmentStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Adjustment is not pending approval");
        }

        // Simply mark as Rejected. No stock movement occurs.
        adjustment.setStatus(AdjustmentStatus.REJECTED);
        stockAdjustmentRepository.save(adjustment);
    }

    @Override
    @Transactional(readOnly = true)
    public StockAdjustmentDetailDto getStockAdjustmentById(Long id) {
        StockAdjustment adjustment = stockAdjustmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Adjustment not found"));

        // Map Items
        List<ItemDetail> itemDetails = adjustment.getAdjustmentItems().stream()
                .map(item -> {
                    String itemName = itemRepository.findNameById(item.getItemId()).orElse("Unknown Item");
                    return ItemDetail.builder()
                            .itemId(item.getItemId())
                            .itemName(itemName)
                            .systemQty(item.getSystemQty())
                            .countedQty(item.getCountedQty())
                            .differenceQty(item.getDifferenceQty())
                            .reasonType(item.getReasonType())
                            .build();
                }).collect(Collectors.toList());

        return StockAdjustmentDetailDto.builder()
                .id(adjustment.getId())
                .adjustmentNumber(adjustment.getAdjustmentNumber())
                .adjustmentDate(adjustment.getAdjustmentDate())
                .status(adjustment.getStatus())
                .warehouseId(adjustment.getWarehouseId())
                .remarks(adjustment.getRemarks())
                .reference(adjustment.getReference())
                .items(itemDetails)
                .build();
    }


    /**
     * This method runs automatically when an event is published
     */
    @EventListener
    @Transactional
    public void onApprovalDecision(ApprovalDecisionEvent event) {

        // 1. Filter: We only care about STOCK_ADJUSTMENT events
        if (event.getType() != ApprovalType.STOCK_ADJUSTMENT) {
            return;
        }

        // 2. Handle the decision
        if (event.getStatus() == ApprovalStatus.APPROVED) {
            approveStockAdjustment(event.getReferenceId());
        } else if (event.getStatus() == ApprovalStatus.REJECTED) {
            rejectStockAdjustment(event.getReferenceId());
        }
    }

    /**
     * Applies the actual inventory changes based on the adjustment items.
     */
    private void applyStockMovements(StockAdjustment adjustment, Long warehouseId) {
        for (StockAdjustmentItem item : adjustment.getAdjustmentItems()) {
            int difference = item.getDifferenceQty();

            if (difference != 0) {
                MovementType movementType = (difference > 0) ? MovementType.IN : MovementType.OUT;

                // Fetch current cost again to be safe, or store it in line item
                // For simplicity, we assume the update service handles cost retrieval or it is passed

                StockUpdateDto updateDto = StockUpdateDto.builder()
                        .itemId(item.getItemId())
                        .warehouseId(warehouseId)
                        .quantity(Math.abs(difference))
                        .transactionType(movementType)
                        .referenceType(ReferenceType.ADJUSTMENT)
                        .referenceId(adjustment.getId())
                        .batchNumber(null) // Pass batch if stored in item
                        .build();

                stockService.updateStock(updateDto);
            }
        }
    }

    private Stock createNewStock(Long itemId, Long warehouseId) {
        return Stock.builder()
                .itemId(itemId)
                .tenantId(getTenantIdOrThrow())
                .warehouseId(warehouseId)
                .averageCost(BigDecimal.ZERO)
                .stockValue(BigDecimal.ZERO)
                .openingQty(0)
                .inQty(0)
                .outQty(0)
                .closingQty(0)
                .build();
    }
}
