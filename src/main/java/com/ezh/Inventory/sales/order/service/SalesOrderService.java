package com.ezh.Inventory.sales.order.service;

import com.ezh.Inventory.sales.order.dto.SalesOrderDto;
import com.ezh.Inventory.sales.order.dto.SalesOrderFilter;
import com.ezh.Inventory.sales.order.dto.SalesConversionReportDto;
import com.ezh.Inventory.sales.order.dto.SalesOrderStats;
import com.ezh.Inventory.sales.order.entity.SalesOrderStatus;
import com.ezh.Inventory.utils.common.CommonFilter;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.exception.CommonException;
import org.springframework.data.domain.Page;

import java.util.List;

public interface SalesOrderService {

    CommonResponse<?> createSalesOrder(SalesOrderDto dto) throws CommonException;

    CommonResponse<?> updateSalesOrder(Long id, SalesOrderDto dto) throws CommonException;

    SalesOrderDto getSalesOrderById(Long id) throws CommonException;

    CommonResponse<?> updateStatus(Long id, SalesOrderStatus status) throws CommonException;

    Page<SalesOrderDto> getAllSalesOrders(SalesOrderFilter filter, int page, int size) throws CommonException;

    List<SalesOrderDto> getAllSalesOrders(SalesOrderFilter filter) throws CommonException;

    SalesOrderStats getStats(CommonFilter filter) throws CommonException;

    byte[] downloadSalesOrdersExcel(SalesOrderFilter filter) throws CommonException;

    SalesConversionReportDto getSalesOrderConversionReport(CommonFilter filter) throws CommonException;

}
