package com.ezh.Inventory.payment.service;

import com.ezh.Inventory.payment.dto.AdvanceCreateDto;
import com.ezh.Inventory.payment.dto.AdvanceDto;
import com.ezh.Inventory.payment.dto.AdvanceRefundRequestDto;
import com.ezh.Inventory.payment.dto.AdvanceUtilizeDto;
import com.ezh.Inventory.utils.common.CommonFilter;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.exception.CommonException;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdvanceService {

    /**
     * Record a new advance deposit from a customer
     */
    CommonResponse<?> createAdvance(AdvanceCreateDto dto) throws CommonException;

    /**
     * Get all advances for a customer
     */
    Page<AdvanceDto> getAllAdvances(Integer page, Integer size, CommonFilter dto) throws CommonException;

    /**
     * Apply a specific advance (or part of it) to an invoice
     */
    CommonResponse<?> utilizeAdvance(AdvanceUtilizeDto dto) throws CommonException;

    /**
     * Initiate a cash refund of unused advance balance — creates refund in PENDING
     * state
     */
    CommonResponse<?> refundAdvance(AdvanceRefundRequestDto dto) throws CommonException;

    /**
     * Mark a PENDING advance refund as CLEARED (money reached the customer)
     */
    CommonResponse<?> confirmAdvanceRefund(Long refundId) throws CommonException;

    /**
     * Single advance record with full utilization and refund history
     */
    AdvanceDto getAdvance(Long advanceId) throws CommonException;

    /**
     * All advances for a customer
     */
    List<AdvanceDto> getAdvancesByCustomer(Long customerId) throws CommonException;
}
