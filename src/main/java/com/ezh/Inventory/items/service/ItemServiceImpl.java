package com.ezh.Inventory.items.service;

import com.ezh.Inventory.items.dto.ItemDto;
import com.ezh.Inventory.items.dto.ItemFilterDto;
import com.ezh.Inventory.items.entity.Item;
import com.ezh.Inventory.items.entity.ItemType;
import com.ezh.Inventory.items.repository.ItemRepository;
import com.ezh.Inventory.items.utils.ItemExcelUtils;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.Status;
import com.ezh.Inventory.utils.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.ezh.Inventory.utils.UserContextUtil.getTenantIdOrThrow;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public CommonResponse createItem(ItemDto itemDto) throws CommonException {
        log.info("Creating new item: {}", itemDto);

        if (itemRepository.existsByItemCode(itemDto.getItemCode())) {
            throw new CommonException("Item Code already exists", HttpStatus.BAD_REQUEST);
        }

        Item item = convertToEntity(itemDto);
        itemRepository.save(item);
        return CommonResponse.builder()
                .id(item.getId().toString())
                .message("")
                .status(Status.SUCCESS)
                .build();
    }

    @Override
    @Transactional
    public CommonResponse updateItem(Long id, ItemDto dto) throws CommonException {
        log.info("Updating item id: {}", id);

        Item item = itemRepository.findByIdAndTenantId(id, getTenantIdOrThrow())
                .orElseThrow(() -> new CommonException("Item not found", HttpStatus.NOT_FOUND));

        mapDtoToEntity(dto, item);
        itemRepository.save(item);

        return CommonResponse.builder()
                .id(item.getId().toString())
                .message("ITEM_UPDATED_SUCCESSFULLY")
                .status(Status.SUCCESS)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ItemDto getItemById(Long id) throws CommonException{
        log.info("Fetching item by id: {}", id);

        Item item = itemRepository.findByIdAndTenantId(id, getTenantIdOrThrow())
                .orElseThrow(() -> new CommonException("Item not found", HttpStatus.NOT_FOUND));

        return convertToDto(item);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItemDto> getAllItems(Integer page, Integer size, ItemFilterDto itemFilterDto) throws CommonException {
        Pageable pageable = PageRequest.of(page, size);
        Long tenantId = getTenantIdOrThrow();

        List<ItemType> types = (itemFilterDto.getItemTypes() != null && !itemFilterDto.getItemTypes().isEmpty())
                ? itemFilterDto.getItemTypes()
                : null;
        // The logic remains clean now without the duplicate assignment
        Page<Item> itemsPage = itemRepository.searchItems(
                tenantId,
                itemFilterDto.getSearchQuery(),
                itemFilterDto.getActive(),
                types,
                itemFilterDto.getBrand(),
                itemFilterDto.getCategory(),
                pageable);
        return itemsPage.map(this::convertToDto);
    }

    @Override
    @Transactional
    public CommonResponse toggleItemActiveStatus(Long itemId, Boolean active) throws CommonException {
        log.info("Updating active status for item {} → {}", itemId, active);

        Item item = itemRepository.findByIdAndTenantId(itemId, getTenantIdOrThrow())
                .orElseThrow(() -> new CommonException("Item not found", HttpStatus.NOT_FOUND));

        // Already same — no update required
        if (item.getIsActive().equals(active)) {
            return CommonResponse.builder()
                    .id(item.getId().toString())
                    .message(active ? "ITEM_ALREADY_ACTIVE" : "ITEM_ALREADY_INACTIVE")
                    .status(Status.SUCCESS)
                    .build();
        }

        // Update status
        item.setIsActive(active);
        itemRepository.save(item);

        return CommonResponse.builder()
                .id(item.getId().toString())
                .message(active ? "ITEM_ACTIVATED" : "ITEM_DEACTIVATED")
                .status(Status.SUCCESS)
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public List<ItemDto> itemSearch(ItemFilterDto itemFilter) throws CommonException {
        List<Item> items = itemRepository.smartSearch(itemFilter.getSearchQuery());

        return items.stream()
                .map(this::convertToDto)
                .toList();
    }


    @Override
    @Transactional
    public CommonResponse<?> saveBulkItems(MultipartFile file) {
        // 1. Validate File
        if (!ItemExcelUtils.hasExcelFormat(file)) {
            throw new CommonException("Invalid File Format", HttpStatus.BAD_REQUEST);
        }

        try {
            List<ItemDto> itemDtos = ItemExcelUtils.excelToItems(file.getInputStream());
            List<Item> itemsToSave = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (ItemDto dto : itemDtos) {
                Item item = null;

                // 2. Check if ID is provided and exists in DB (UPDATE SCENARIO)
                if (dto.getId() != null) {
                    Optional<Item> existingItem = itemRepository.findByIdAndTenantId(dto.getId(), getTenantIdOrThrow());

                    if (existingItem.isPresent()) {
                        item = existingItem.get();
                        mapDtoToEntity(dto, item); // Update existing fields
                    }
                    // IF NOT PRESENT: 'item' remains null, so we fall through to creation below.
                }

                // 3. If item is still null (ID was null OR ID was not found), treat as CREATE
                if (item == null) {
                    // Check business key (Item Code) uniqueness
                    if (itemRepository.existsByItemCode(dto.getItemCode())) {
                        errors.add("Item Code " + dto.getItemCode() + " already exists. Skipped.");
                        continue;
                    }

                    item = convertToEntity(dto);
                    // IMPORTANT: Ensure ID is null so DB generates a fresh one
                    // (This ignores the invalid ID provided in the Excel file)
                    item.setId(null);
                }

                itemsToSave.add(item);
            }

            // 4. Batch Save
            itemRepository.saveAll(itemsToSave);

            return CommonResponse.builder()
                    .message("Processed " + itemsToSave.size() + " items. " + errors.size() + " errors.")
                    .data(errors)
                    .status(Status.SUCCESS)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Excel processing failed: " + e.getMessage());
        }
    }

    @Override
    public ByteArrayInputStream loadItemsForDownload(ItemFilterDto itemFilter) throws CommonException {
        // SECURITY: Always filter by Tenant ID
        List<Item> items = itemRepository.findAll();

        return ItemExcelUtils.itemsToExcel(items);
    }

    @Override
    public ByteArrayInputStream getItemTemplate() throws CommonException {
        // Simply call the helper to generate the blank/sample file
        return ItemExcelUtils.generateExcelTemplate();
    }

    private Item convertToEntity(ItemDto dto) {
        return Item.builder()
                .name(dto.getName())
                .tenantId(getTenantIdOrThrow())
                .itemCode(dto.getItemCode())
                .sku(dto.getSku())
                .barcode(dto.getBarcode())
                .itemType(dto.getItemType())
                .category(dto.getCategory())
                .brand(dto.getBrand())
                .manufacturer(dto.getManufacturer()) // if manufacturer stored in "model" field, else change accordingly
                .unitOfMeasure(dto.getUnitOfMeasure())
                .purchasePrice(dto.getPurchasePrice())
                .sellingPrice(dto.getSellingPrice())
                .mrp(dto.getMrp())
                .taxPercentage(dto.getTaxPercentage())
                .discountPercentage(dto.getDiscountPercentage())
                .hsnSacCode(dto.getHsnSacCode())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .isActive(dto.getIsActive())
                .build();
    }


    private ItemDto convertToDto(Item item) {
        return ItemDto.builder()
                .id(item.getId())
                .name(item.getName())
                .itemCode(item.getItemCode())
                .sku(item.getSku())
                .barcode(item.getBarcode())
                .itemType(item.getItemType())
                .category(item.getCategory())
                .brand(item.getBrand())
                .manufacturer(item.getManufacturer())
                .unitOfMeasure(item.getUnitOfMeasure())
                .purchasePrice(item.getPurchasePrice())
                .sellingPrice(item.getSellingPrice())
                .mrp(item.getMrp())
                .taxPercentage(item.getTaxPercentage())
                .discountPercentage(item.getDiscountPercentage())
                .hsnSacCode(item.getHsnSacCode())
                .description(item.getDescription())
                .imageUrl(item.getImageUrl())
                .isActive(item.getIsActive())
                .build();
    }

    private void mapDtoToEntity(ItemDto dto, Item item) {
        item.setName(dto.getName());
        item.setItemCode(dto.getItemCode());
        item.setSku(dto.getSku());
        item.setBarcode(dto.getBarcode());
        item.setItemType(dto.getItemType());
        item.setCategory(dto.getCategory());
        item.setBrand(dto.getBrand());
        item.setManufacturer(dto.getManufacturer());
        item.setUnitOfMeasure(dto.getUnitOfMeasure());
        item.setPurchasePrice(dto.getPurchasePrice());
        item.setSellingPrice(dto.getSellingPrice());
        item.setMrp(dto.getMrp());
        item.setTaxPercentage(dto.getTaxPercentage());
        item.setDiscountPercentage(dto.getDiscountPercentage());
        item.setHsnSacCode(dto.getHsnSacCode());
        item.setDescription(dto.getDescription());
        item.setImageUrl(dto.getImageUrl());
        item.setIsActive(dto.getIsActive());
    }
}
