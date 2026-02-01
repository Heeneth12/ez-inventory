package com.ezh.Inventory.purchase.prq.service;

import com.ezh.Inventory.purchase.prq.dto.PurchaseRequestDto;
import com.ezh.Inventory.purchase.prq.entity.PrqStatus;
import com.ezh.Inventory.utils.common.CommonFilter;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.exception.CommonException;
import org.springframework.data.domain.Page;

public interface PurchaseRequestService {

    CommonResponse<?> createPrq(PurchaseRequestDto dto) throws CommonException;
    CommonResponse<?> updateStatus(Long prqId, PrqStatus status) throws CommonException;
    CommonResponse<?> updatePrq(Long prqId, PurchaseRequestDto dto) throws CommonException;
    Page<PurchaseRequestDto> getAllPrqs(Integer page, Integer size, CommonFilter filter) throws CommonException;
    PurchaseRequestDto getPrqById(Long prqId) throws CommonException;
}
