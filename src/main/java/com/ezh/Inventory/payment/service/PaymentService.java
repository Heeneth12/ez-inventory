package com.ezh.Inventory.payment.service;

import com.ezh.Inventory.payment.dto.*;
import com.ezh.Inventory.utils.common.CommonFilter;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.exception.CommonException;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PaymentService {

    /**
     * Record a payment against one or more invoices.
     * Rule: sum(allocations.amountToPay) must equal dto.totalAmount exactly.
     */
    CommonResponse<?> recordPayment(PaymentCreateDto dto) throws CommonException;

    Page<PaymentDto> getAllPayments(PaymentFilter filter, Integer page, Integer size) throws CommonException;

    PaymentDto getPayment(Long paymentId) throws CommonException;

    List<InvoicePaymentHistoryDto> getPaymentsByInvoiceId(Long invoiceId) throws CommonException;

    InvoicePaymentSummaryDto getInvoicePaymentSummary(Long invoiceId) throws CommonException;

    CustomerFinancialSummaryDto getCustomerFinancialSummary(Long customerId) throws CommonException;

    PaymentStats getStats(CommonFilter filter) throws CommonException;
}
