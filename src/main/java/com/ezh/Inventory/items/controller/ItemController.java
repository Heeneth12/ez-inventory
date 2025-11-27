package com.ezh.Inventory.items.controller;

import com.ezh.Inventory.items.dto.ItemDto;
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

@Slf4j
@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse> createItem(@RequestBody ItemDto itemDto) throws CommonException {
        log.info("Creating new item with SKU: {}", itemDto);
        CommonResponse response = itemService.createItem(itemDto);
        return ResponseResource.success(HttpStatus.CREATED, response, "ITEM CREATED SUCCESSFULLY");
    }

    @PostMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<Page<ItemDto>> getAllItems(@RequestParam Integer page, @RequestParam Integer size) throws CommonException {
        log.info("Fetching all items");
        Page<ItemDto> response = itemService.getAllItems(page, size);
        return ResponseResource.success(HttpStatus.OK, response, "ITEM LIST FETCHED");
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


//    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<?> searchItems(@RequestParam("query") String query) {
//        log.info("Searching items by query: {}", query);
//        List<ItemResponse> results = itemService.search(query);
//        return ResponseResource.success(HttpStatus.OK, results, "SEARCH RESULTS");
//    }
}
