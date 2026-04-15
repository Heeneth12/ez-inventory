package com.ezh.Inventory.payment.service;

import com.ezh.Inventory.payment.dto.AdvanceCreateDto;
import com.ezh.Inventory.payment.dto.AdvanceDto;
import com.ezh.Inventory.payment.dto.AdvanceRefundRequestDto;
import com.ezh.Inventory.payment.dto.AdvanceUtilizeDto;
import com.ezh.Inventory.payment.entity.AdvanceRefund;
import com.ezh.Inventory.payment.entity.AdvanceUtilization;
import com.ezh.Inventory.payment.entity.CustomerAdvance;
import com.ezh.Inventory.payment.entity.Payment;
import com.ezh.Inventory.payment.entity.PaymentAllocation;
import com.ezh.Inventory.payment.repository.AdvanceRefundRepository;
import com.ezh.Inventory.payment.repository.AdvanceUtilizationRepository;
import com.ezh.Inventory.payment.repository.CustomerAdvanceRepository;
import com.ezh.Inventory.payment.repository.PaymentRepository;
import com.ezh.Inventory.payment.repository.PaymentAllocationRepository;
import com.ezh.Inventory.sales.invoice.entity.Invoice;
import com.ezh.Inventory.sales.invoice.entity.InvoicePaymentStatus;
import com.ezh.Inventory.sales.invoice.repository.InvoiceRepository;
import com.ezh.Inventory.payment.entity.enums.AdvanceStatus;
import com.ezh.Inventory.payment.entity.enums.PaymentMethod;
import com.ezh.Inventory.payment.entity.enums.PaymentStatus;
import com.ezh.Inventory.payment.entity.enums.RefundStatus;
import com.ezh.Inventory.payment.entity.enums.UtilizationStatus;
import com.ezh.Inventory.utils.UserContextUtil;
import com.ezh.Inventory.utils.common.DocPrefix;
import com.ezh.Inventory.utils.common.DocumentNumberUtil;
import com.ezh.Inventory.utils.common.CommonFilter;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.Status;
import com.ezh.Inventory.utils.exception.BadRequestException;
import com.ezh.Inventory.utils.exception.CommonException;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdvanceServiceImpl implements AdvanceService {

    private final CustomerAdvanceRepository advanceRepository;
    private final AdvanceUtilizationRepository utilizationRepository;
    private final AdvanceRefundRepository refundRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;

    @Override
    @Transactional
    public CommonResponse<?> createAdvance(AdvanceCreateDto dto) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        CustomerAdvance advance = CustomerAdvance.builder()
                .tenantId(tenantId)
                .advanceNumber(DocumentNumberUtil.generate(DocPrefix.ADV))
                .customerId(dto.getCustomerId())
                .receivedDate(new Date())
                .amount(dto.getAmount())
                .availableBalance(dto.getAmount())
                .paymentMethod(dto.getPaymentMethod())
                .status(AdvanceStatus.CONFIRMED)
                .referenceNumber(dto.getReferenceNumber())
                .bankName(dto.getBankName())
                .remarks(dto.getRemarks())
                .build();

        advanceRepository.save(advance);

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .id(advance.getId().toString())
                .message("Advance recorded successfully")
                .build();
    }

    @Override
    public Page<AdvanceDto> getAllAdvances(Integer page, Integer size, CommonFilter dto) throws CommonException {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<CustomerAdvance> advancePage = advanceRepository.findByTenantId(tenantId, pageable);

        return advancePage.map(advance -> mapToDto(advance, false));
    }


    @Override
    @Transactional
    public CommonResponse<?> utilizeAdvance(AdvanceUtilizeDto dto) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        CustomerAdvance advance = advanceRepository.findByIdAndTenantId(dto.getAdvanceId(), tenantId)
                .orElseThrow(() -> new CommonException("Advance not found", HttpStatus.NOT_FOUND));

        if (advance.getStatus() == AdvanceStatus.CANCELLED
                || advance.getStatus() == AdvanceStatus.FULLY_UTILIZED) {
            throw new BadRequestException(
                    "Advance " + advance.getAdvanceNumber() + " is not available for utilization");
        }

        if (advance.getAvailableBalance().compareTo(dto.getAmount()) < 0) {
            throw new BadRequestException(
                    "Insufficient advance balance. Available: " + advance.getAvailableBalance());
        }

        Invoice invoice = invoiceRepository.findByIdAndTenantId(dto.getInvoiceId(), tenantId)
                .orElseThrow(() -> new CommonException("Invoice not found", HttpStatus.NOT_FOUND));

        if (dto.getAmount().compareTo(invoice.getBalance()) > 0) {
            throw new BadRequestException(
                    "Amount exceeds invoice balance. Invoice due: " + invoice.getBalance());
        }

        // Create utilization record
        AdvanceUtilization utilization = AdvanceUtilization.builder()
                .tenantId(tenantId)
                .advance(advance)
                .invoiceId(dto.getInvoiceId())
                .utilizedAmount(dto.getAmount())
                .utilizationDate(new Date())
                .status(UtilizationStatus.CONFIRMED)
                .remarks(dto.getRemarks())
                .build();

        utilizationRepository.save(utilization);

        // Reduce advance balance
        advance.setAvailableBalance(advance.getAvailableBalance().subtract(dto.getAmount()));
        advance.setStatus(
                advance.getAvailableBalance().compareTo(BigDecimal.ZERO) == 0
                        ? AdvanceStatus.FULLY_UTILIZED
                        : AdvanceStatus.PARTIALLY_UTILIZED);
        advanceRepository.save(advance);

        // ── Revenue ledger ────────────────────────────────────────────────────────
        // Create a CONFIRMED Payment so this amount appears in revenue reports,
        // invoice payment history, and the All Payments table.
        Payment payment = Payment.builder()
                .tenantId(tenantId)
                .paymentNumber(DocumentNumberUtil.generate(DocPrefix.PAY))
                .customerId(advance.getCustomerId())
                .paymentDate(new Date())
                .amount(dto.getAmount())
                .paymentMethod(PaymentMethod.ADVANCE)
                .status(PaymentStatus.CONFIRMED)
                .referenceNumber(advance.getAdvanceNumber())
                .remarks("Advance " + advance.getAdvanceNumber() + " applied to invoice")
                .build();
        paymentRepository.save(payment);

        PaymentAllocation allocation = PaymentAllocation.builder()
                .tenantId(tenantId)
                .payment(payment)
                .invoiceId(dto.getInvoiceId())
                .allocatedAmount(dto.getAmount())
                .allocationDate(new Date())
                .build();
        paymentAllocationRepository.save(allocation);

        // Update invoice
        updateInvoiceBalance(invoice, dto.getAmount());

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .message("Advance applied to invoice successfully")
                .build();
    }

    @Override
    @Transactional
    public CommonResponse<?> refundAdvance(AdvanceRefundRequestDto dto) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        CustomerAdvance advance = advanceRepository.findByIdAndTenantId(dto.getAdvanceId(), tenantId)
                .orElseThrow(() -> new CommonException("Advance not found", HttpStatus.NOT_FOUND));

        if (advance.getAvailableBalance().compareTo(dto.getRefundAmount()) < 0) {
            throw new BadRequestException("Refund amount exceeds available balance. Available: "
                    + advance.getAvailableBalance());
        }

        // Write refund record in PENDING state — cleared separately once cash reaches
        // customer
        AdvanceRefund refund = AdvanceRefund.builder()
                .tenantId(tenantId)
                .refundNumber(DocumentNumberUtil.generate(DocPrefix.RFD))
                .advance(advance)
                .refundAmount(dto.getRefundAmount())
                .refundDate(new Date())
                .refundMethod(dto.getRefundMethod())
                .refundReferenceNumber(dto.getRefundReferenceNumber())
                .status(RefundStatus.PENDING)
                .remarks(dto.getRemarks())
                .build();

        refundRepository.save(refund);

        // Reduce advance balance immediately on initiation
        advance.setAvailableBalance(advance.getAvailableBalance().subtract(dto.getRefundAmount()));
        if (advance.getAvailableBalance().compareTo(BigDecimal.ZERO) == 0) {
            advance.setStatus(AdvanceStatus.REFUNDED);
        }
        advanceRepository.save(advance);

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .id(refund.getId().toString())
                .message("Refund initiated successfully. Refund No: " + refund.getRefundNumber())
                .build();
    }

    @Override
    @Transactional
    public CommonResponse<?> confirmAdvanceRefund(Long refundId) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        AdvanceRefund refund = refundRepository.findByIdAndTenantId(refundId, tenantId)
                .orElseThrow(() -> new CommonException("Refund record not found",
                        HttpStatus.NOT_FOUND));

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new BadRequestException("Refund is already " + refund.getStatus());
        }

        refund.setStatus(RefundStatus.CLEARED);
        refundRepository.save(refund);

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .message("Refund marked as cleared")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdvanceDto getAdvance(Long advanceId) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        CustomerAdvance advance = advanceRepository.findByIdAndTenantId(advanceId, tenantId)
                .orElseThrow(() -> new CommonException("Advance not found", HttpStatus.NOT_FOUND));
        return mapToDto(advance, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdvanceDto> getAdvancesByCustomer(Long customerId) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        return advanceRepository.findByCustomerIdAndTenantIdOrderByReceivedDateDesc(customerId, tenantId)
                .stream()
                .map(a -> mapToDto(a, false))
                .collect(Collectors.toList());
    }

    // PRIVATE HELPERS

    private void updateInvoiceBalance(Invoice invoice, BigDecimal paidAmount) {
        BigDecimal newPaid = invoice.getAmountPaid().add(paidAmount);
        BigDecimal newBalance = invoice.getGrandTotal().subtract(newPaid);
        invoice.setAmountPaid(newPaid);
        invoice.setBalance(newBalance);
        invoice.setPaymentStatus(newBalance.compareTo(BigDecimal.ZERO) <= 0
                ? InvoicePaymentStatus.PAID
                : InvoicePaymentStatus.PARTIALLY_PAID);
        invoiceRepository.save(invoice);
    }

    private AdvanceDto mapToDto(CustomerAdvance advance, boolean includeDetails) {
        AdvanceDto dto = AdvanceDto.builder()
                .id(advance.getId())
                .advanceNumber(advance.getAdvanceNumber())
                .customerId(advance.getCustomerId())
                .receivedDate(advance.getReceivedDate())
                .amount(advance.getAmount())
                .availableBalance(advance.getAvailableBalance())
                .paymentMethod(advance.getPaymentMethod())
                .status(advance.getStatus())
                .referenceNumber(advance.getReferenceNumber())
                .bankName(advance.getBankName())
                .remarks(advance.getRemarks())
                .build();

        if (includeDetails) {
            dto.setUtilizations(advance.getUtilizations().stream()
                    .map(u -> AdvanceDto.UtilizationItem.builder()
                            .id(u.getId())
                            .invoiceId(u.getInvoiceId())
                            .utilizedAmount(u.getUtilizedAmount())
                            .utilizationDate(u.getUtilizationDate())
                            .status(u.getStatus())
                            .build())
                    .collect(Collectors.toList()));

            dto.setRefunds(advance.getRefunds().stream()
                    .map(r -> AdvanceDto.RefundItem.builder()
                            .id(r.getId())
                            .refundNumber(r.getRefundNumber())
                            .refundAmount(r.getRefundAmount())
                            .refundDate(r.getRefundDate())
                            .refundMethod(r.getRefundMethod())
                            .refundReferenceNumber(r.getRefundReferenceNumber())
                            .status(r.getStatus())
                            .build())
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}
