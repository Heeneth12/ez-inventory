package com.ezh.Inventory.stock.service;

import com.ezh.Inventory.stock.dto.StockAdjustmentCreateDto;
import com.ezh.Inventory.stock.dto.StockAdjustmentDetailDto;
import com.ezh.Inventory.stock.dto.StockAdjustmentListDto;
import com.ezh.Inventory.stock.dto.StockFilterDto;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.exception.CommonException;
import org.springframework.data.domain.Page;

public interface StockAdjustmentService {

    CommonResponse<?> createStockAdjustment(StockAdjustmentCreateDto dto) throws CommonException;

    Page<StockAdjustmentListDto> getAllStockAdjustments(StockFilterDto filter, Integer page, Integer size);

    StockAdjustmentDetailDto getStockAdjustmentById(Long id);

    void approveStockAdjustment(Long adjustmentId) throws CommonException;

    void rejectStockAdjustment(Long adjustmentId) throws CommonException;

}

