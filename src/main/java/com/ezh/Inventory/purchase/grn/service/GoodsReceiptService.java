package com.ezh.Inventory.purchase.grn.service;

import com.ezh.Inventory.purchase.grn.dto.GrnDto;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.exception.CommonException;
import org.springframework.data.domain.Page;

import java.util.List;

public interface GoodsReceiptService {

    CommonResponse createAndApproveGrn(GrnDto dto) throws CommonException;

    GrnDto getGrnDetails(Long grnId) throws CommonException;

    List<GrnDto> getGrnHistoryForPo(Long purchaseOrderId);

    Page<GrnDto> getAllGrns(Integer page, Integer size);
}
