package com.ezh.Inventory.items.service;

import com.ezh.Inventory.items.dto.ItemDto;
import com.ezh.Inventory.items.dto.ItemFilterDto;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.exception.CommonException;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ItemService {

    CommonResponse createItem(ItemDto itemDto);
    CommonResponse updateItem(Long id, ItemDto itemDto);
    ItemDto getItemById(Long id);
    Page<ItemDto> getAllItems(Integer page, Integer size, ItemFilterDto itemFilterDto);
    CommonResponse toggleItemActiveStatus(Long id, Boolean active);
    List<ItemDto> itemSearch(ItemFilterDto itemFilter) throws CommonException;

}
