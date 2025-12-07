package com.ezh.Inventory.purchase.returns.service;

import com.ezh.Inventory.purchase.returns.dto.PurchaseReturnDto;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.exception.CommonException;
import org.springframework.data.domain.Page;

public interface PurchaseReturnService {

    CommonResponse createPurchaseReturn(PurchaseReturnDto dto) throws CommonException;

    PurchaseReturnDto getReturnDetails(Long returnId) throws CommonException;

    Page<PurchaseReturnDto> getAllReturns(Integer page, Integer size);

}
