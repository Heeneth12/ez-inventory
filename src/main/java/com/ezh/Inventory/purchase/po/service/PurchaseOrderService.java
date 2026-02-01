package com.ezh.Inventory.purchase.po.service;

import com.ezh.Inventory.purchase.po.dto.PurchaseOrderDto;
import com.ezh.Inventory.purchase.po.dto.PurchaseOrderFilter;
import com.ezh.Inventory.purchase.po.entity.PoStatus;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.exception.CommonException;
import org.springframework.data.domain.Page;

public interface PurchaseOrderService {

    CommonResponse createPurchaseOrder(PurchaseOrderDto dto);
    PurchaseOrderDto getPurchaseOrderById(Long id) throws CommonException;
    Page<PurchaseOrderDto> getAllPurchaseOrders(Integer page, Integer size, PurchaseOrderFilter filter);
    CommonResponse updatePurchaseOrder(Long poId, PurchaseOrderDto dto) throws CommonException;
    CommonResponse updatePurchaseOrderStatus(Long poId, PoStatus status) throws CommonException;


}
