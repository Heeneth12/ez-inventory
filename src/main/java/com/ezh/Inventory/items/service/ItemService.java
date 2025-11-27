package com.ezh.Inventory.items.service;

import com.ezh.Inventory.items.dto.ItemDto;
import com.ezh.Inventory.utils.common.CommonResponse;
import org.springframework.data.domain.Page;

public interface ItemService {

    CommonResponse createItem(ItemDto itemDto);
    CommonResponse updateItem(Long id, ItemDto itemDto);
    ItemDto getItemById(Long id);
    Page<ItemDto> getAllItems(Integer page, Integer size);
    CommonResponse toggleItemActiveStatus(Long id, Boolean active);

}
