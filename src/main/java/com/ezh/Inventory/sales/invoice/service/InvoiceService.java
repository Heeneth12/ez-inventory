package com.ezh.Inventory.sales.invoice.service;

import com.ezh.Inventory.sales.invoice.dto.InvoiceCreateDto;
import com.ezh.Inventory.sales.invoice.dto.InvoiceDto;
import com.ezh.Inventory.sales.invoice.dto.InvoiceFilter;
import com.ezh.Inventory.sales.invoice.entity.InvoiceStatus;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.exception.CommonException;
import org.springframework.data.domain.Page;

import java.util.List;

public interface InvoiceService {

   CommonResponse createInvoice(InvoiceCreateDto dto) throws CommonException;
   CommonResponse updateInvoice(Long id, InvoiceCreateDto dto) throws CommonException;
   InvoiceDto getInvoiceById(Long invoiceId) throws CommonException;
   Page<InvoiceDto> getAllInvoices(InvoiceFilter filter, Integer page, Integer size) throws CommonException;
   List<InvoiceDto> searchInvoices(InvoiceFilter filter) throws CommonException;
   CommonResponse updateInvoiceStatus(Long invoiceId, InvoiceStatus status) throws CommonException;
}
