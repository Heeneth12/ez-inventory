package com.ezh.Inventory.sales.returns.service;

import com.ezh.Inventory.sales.returns.dto.SalesReturnDto;
import com.ezh.Inventory.sales.returns.dto.SalesReturnRequestDto;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.exception.CommonException;
import org.springframework.data.domain.Page;

public interface SalesReturnService {

    CommonResponse createSalesReturn(SalesReturnRequestDto request)  throws CommonException;

    Page<SalesReturnDto> getSalesReturns(Integer page, Integer size) throws CommonException;

    SalesReturnDto getSalesReturnById(Long id) throws CommonException;

}
