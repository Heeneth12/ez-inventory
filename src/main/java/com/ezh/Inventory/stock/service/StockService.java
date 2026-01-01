package com.ezh.Inventory.stock.service;


import com.ezh.Inventory.stock.dto.*;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.exception.CommonException;
import org.springframework.data.domain.Page;

import java.util.List;

public interface StockService {

    CommonResponse updateStock(StockUpdateDto stockUpdateDto);
    Page<StockDto> getCurrentStock(StockFilterDto filterDto, Integer page, Integer size);
    Page<StockLedgerDto> getStockTransactions(StockFilterDto filterDto, Integer page, Integer size);
    List<ItemStockSearchDto> searchItemsWithBatches(String query, Long warehouseId);
    StockDashboardDto getStockDashboard(Long warehouseId) throws CommonException;

}
