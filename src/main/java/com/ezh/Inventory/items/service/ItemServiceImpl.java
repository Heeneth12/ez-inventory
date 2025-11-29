package com.ezh.Inventory.items.service;

import com.ezh.Inventory.items.dto.ItemDto;
import com.ezh.Inventory.items.dto.ItemFilterDto;
import com.ezh.Inventory.items.entity.Item;
import com.ezh.Inventory.items.repository.ItemRepository;
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

import java.util.List;

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

        Item item = itemRepository.findById(id)
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

        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new CommonException("Item not found", HttpStatus.NOT_FOUND));

        return convertToDto(item);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItemDto> getAllItems(Integer page, Integer size, ItemFilterDto itemFilterDto) throws CommonException {
        log.info("Fetching all items");
        Pageable pageable = PageRequest.of(page, size);

        Page<Item> itemsPage = itemRepository.searchItems(
                itemFilterDto.getSearchQuery(),
                itemFilterDto.getActive(),
                itemFilterDto.getItemType(),
                itemFilterDto.getBrand(),
                itemFilterDto.getCategory(),
                pageable);

        return itemsPage.map(this::convertToDto);
    }

    @Override
    @Transactional
    public CommonResponse toggleItemActiveStatus(Long itemId, Boolean active) throws CommonException{
        log.info("Updating active status for item {} → {}", itemId, active);

        Item item = itemRepository.findById(itemId)
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



    private Item convertToEntity(ItemDto dto) {
        return Item.builder()
                .name(dto.getName())
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
