package com.ezh.Inventory.items.controller;

import com.ezh.Inventory.items.dto.ItemDto;
import com.ezh.Inventory.items.dto.ItemFilterDto;
import com.ezh.Inventory.items.service.ItemService;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.ResponseResource;
import com.ezh.Inventory.utils.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse> createItem(@RequestBody ItemDto itemDto) throws CommonException {
        log.info("Entering createItem with : {}", itemDto);
        CommonResponse response = itemService.createItem(itemDto);
        return ResponseResource.success(HttpStatus.CREATED, response, "Item created successfully");
    }

    @PostMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<Page<ItemDto>> getAllItems(@RequestParam Integer page, @RequestParam Integer size,
        @RequestBody ItemFilterDto itemFilterDto ) throws CommonException {
        log.info("Entering getAllItems with page: {}, size: {}, filter {}", page, size, itemFilterDto);
        Page<ItemDto> response = itemService.getAllItems(page, size, itemFilterDto);
        return ResponseResource.success(HttpStatus.OK, response, "Item list fetched successfully");
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<ItemDto> getItemById(@PathVariable Long id) throws CommonException {
        log.info("Fetching item with ID: {}", id);
        ItemDto response = itemService.getItemById(id);
        return ResponseResource.success(HttpStatus.OK, response, "ITEM DETAILS FETCHED");
    }

    @PostMapping(value = "/{id}/update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse> updateItem(@PathVariable Long id, @RequestBody ItemDto itemDto) throws CommonException {
        log.info("Updating item with ID: {}", id);
        CommonResponse response = itemService.updateItem(id, itemDto);
        return ResponseResource.success(HttpStatus.OK, response, "ITEM UPDATED SUCCESSFULLY");
    }

    @PostMapping(value = "/{id}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse> toggleItemActiveStatus(@PathVariable Long id, @RequestParam Boolean active) throws CommonException {
        log.info("Soft deleting item with ID: {}", id);
        CommonResponse response = itemService.toggleItemActiveStatus(id, active);
        return ResponseResource.success(HttpStatus.OK, response, "ITEM TOGGLED SUCCESSFULLY");
    }

    @PostMapping(value = "/search",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<List<ItemDto>> searchItems(@RequestBody ItemFilterDto itemFilter) throws CommonException {
        log.info("Searching items by query: {}", itemFilter);
        List<ItemDto> response = itemService.itemSearch(itemFilter);
        return ResponseResource.success(HttpStatus.OK, response, "SEARCH RESULTS");
    }
}
