package com.ezh.Inventory.payment.service;

import com.ezh.Inventory.payment.dto.AdvanceDto;
import com.ezh.Inventory.payment.dto.CreditNoteDto;
import com.ezh.Inventory.payment.dto.CreditNoteRefundRequestDto;
import com.ezh.Inventory.payment.dto.CreditNoteUtilizeDto;
import com.ezh.Inventory.payment.entity.CreditNote;
import com.ezh.Inventory.payment.entity.CreditNoteRefund;
import com.ezh.Inventory.payment.entity.CreditNoteUtilization;
import com.ezh.Inventory.payment.entity.CustomerAdvance;
import com.ezh.Inventory.payment.entity.Payment;
import com.ezh.Inventory.payment.entity.PaymentAllocation;
import com.ezh.Inventory.payment.repository.CreditNoteRefundRepository;
import com.ezh.Inventory.payment.repository.CreditNoteRepository;
import com.ezh.Inventory.payment.repository.CreditNoteUtilizationRepository;
import com.ezh.Inventory.payment.repository.PaymentRepository;
import com.ezh.Inventory.payment.repository.PaymentAllocationRepository;
import com.ezh.Inventory.sales.invoice.entity.Invoice;
import com.ezh.Inventory.sales.invoice.entity.InvoicePaymentStatus;
import com.ezh.Inventory.sales.invoice.repository.InvoiceRepository;
import com.ezh.Inventory.payment.entity.enums.CreditNoteStatus;
import com.ezh.Inventory.payment.entity.enums.PaymentMethod;
import com.ezh.Inventory.payment.entity.enums.PaymentStatus;
import com.ezh.Inventory.payment.entity.enums.RefundStatus;
import com.ezh.Inventory.payment.entity.enums.UtilizationStatus;
import com.ezh.Inventory.utils.UserContextUtil;
import com.ezh.Inventory.utils.common.*;
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
public class CreditNoteServiceImpl implements CreditNoteService {

    private final CreditNoteRepository creditNoteRepository;
    private final CreditNoteUtilizationRepository utilizationRepository;
    private final CreditNoteRefundRepository refundRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;

    // CREATE CREDIT NOTE (called internally by SalesReturn service)
    @Override
    @Transactional
    public CommonResponse<?> createCreditNote(Long customerId, BigDecimal amount, Long sourceReturnId)
            throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        CreditNote creditNote = CreditNote.builder()
                .tenantId(tenantId)
                .creditNoteNumber(DocumentNumberUtil.generate(DocPrefix.CN))
                .customerId(customerId)
                .sourceReturnId(sourceReturnId)
                .issueDate(new Date())
                .amount(amount)
                .availableBalance(amount)
                .status(CreditNoteStatus.ISSUED)
                .remarks("Auto-generated from Sales Return #" + sourceReturnId)
                .build();

        creditNoteRepository.save(creditNote);

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .id(creditNote.getId().toString())
                .message("Credit Note " + creditNote.getCreditNoteNumber() + " created")
                .build();
    }


    @Override
    public Page<CreditNoteDto> getAllCreditNote(Integer page, Integer size, CommonFilter dto) throws CommonException {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<CreditNote> creditNotes  = creditNoteRepository.findByTenantId(tenantId, pageable);

        return creditNotes.map(creditNote -> mapToDto(creditNote, false));
    }

    // UTILIZE — apply CN to an invoice
    @Override
    @Transactional
    public CommonResponse<?> utilizeCreditNote(CreditNoteUtilizeDto dto) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        CreditNote creditNote = creditNoteRepository.findByIdAndTenantId(dto.getCreditNoteId(), tenantId)
                .orElseThrow(() -> new CommonException("Credit Note not found", HttpStatus.NOT_FOUND));

        if (creditNote.getStatus() == CreditNoteStatus.CANCELLED
                || creditNote.getStatus() == CreditNoteStatus.FULLY_UTILIZED) {
            throw new BadRequestException("Credit Note " + creditNote.getCreditNoteNumber() + " is not available");
        }

        if (creditNote.getAvailableBalance().compareTo(dto.getAmount()) < 0) {
            throw new BadRequestException("Insufficient credit note balance. Available: "
                    + creditNote.getAvailableBalance());
        }

        Invoice invoice = invoiceRepository.findByIdAndTenantId(dto.getInvoiceId(), tenantId)
                .orElseThrow(() -> new CommonException("Invoice not found", HttpStatus.NOT_FOUND));

        if (dto.getAmount().compareTo(invoice.getBalance()) > 0) {
            throw new BadRequestException("Amount exceeds invoice balance. Invoice due: " + invoice.getBalance());
        }

        // Create utilization record
        CreditNoteUtilization utilization = CreditNoteUtilization.builder()
                .tenantId(tenantId)
                .creditNote(creditNote)
                .invoiceId(dto.getInvoiceId())
                .utilizedAmount(dto.getAmount())
                .utilizationDate(new Date())
                .status(UtilizationStatus.CONFIRMED)
                .remarks(dto.getRemarks())
                .build();

        utilizationRepository.save(utilization);

        // Reduce CN balance
        creditNote.setAvailableBalance(creditNote.getAvailableBalance().subtract(dto.getAmount()));
        creditNote.setStatus(
                creditNote.getAvailableBalance().compareTo(BigDecimal.ZERO) == 0
                        ? CreditNoteStatus.FULLY_UTILIZED
                        : CreditNoteStatus.PARTIALLY_UTILIZED
        );
        creditNoteRepository.save(creditNote);

        // ── Revenue ledger ────────────────────────────────────────────────────────
        // Create a CONFIRMED Payment so this amount appears in revenue reports,
        // invoice payment history, and the All Payments table.
        Payment payment = Payment.builder()
                .tenantId(tenantId)
                .paymentNumber(DocumentNumberUtil.generate(DocPrefix.PAY))
                .customerId(creditNote.getCustomerId())
                .paymentDate(new Date())
                .amount(dto.getAmount())
                .paymentMethod(PaymentMethod.CREDIT_NOTE)
                .status(PaymentStatus.CONFIRMED)
                .referenceNumber(creditNote.getCreditNoteNumber())
                .remarks("Credit Note " + creditNote.getCreditNoteNumber() + " applied to invoice")
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
        // ─────────────────────────────────────────────────────────────────────────

        // Update invoice
        updateInvoiceBalance(invoice, dto.getAmount());

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .message("Credit note applied to invoice successfully")
                .build();
    }

    // REFUND — pay back CN value as cash
    @Override
    @Transactional
    public CommonResponse<?> refundCreditNote(CreditNoteRefundRequestDto dto) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        CreditNote creditNote = creditNoteRepository.findByIdAndTenantId(dto.getCreditNoteId(), tenantId)
                .orElseThrow(() -> new CommonException("Credit Note not found", HttpStatus.NOT_FOUND));

        if (creditNote.getAvailableBalance().compareTo(dto.getRefundAmount()) < 0) {
            throw new BadRequestException("Refund amount exceeds available balance. Available: "
                    + creditNote.getAvailableBalance());
        }

        CreditNoteRefund refund = CreditNoteRefund.builder()
                .tenantId(tenantId)
                .refundNumber(DocumentNumberUtil.generate(DocPrefix.RFD))
                .creditNote(creditNote)
                .refundAmount(dto.getRefundAmount())
                .refundDate(new Date())
                .refundMethod(dto.getRefundMethod())
                .refundReferenceNumber(dto.getRefundReferenceNumber())
                .status(RefundStatus.PENDING)
                .remarks(dto.getRemarks())
                .build();

        refundRepository.save(refund);

        creditNote.setAvailableBalance(creditNote.getAvailableBalance().subtract(dto.getRefundAmount()));
        if (creditNote.getAvailableBalance().compareTo(BigDecimal.ZERO) == 0) {
            creditNote.setStatus(CreditNoteStatus.REFUNDED);
        }
        creditNoteRepository.save(creditNote);

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .id(refund.getId().toString())
                .message("Credit note refund initiated. Refund No: " + refund.getRefundNumber())
                .build();
    }

    // CONFIRM REFUND
    @Override
    @Transactional
    public CommonResponse<?> confirmCreditNoteRefund(Long refundId) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        CreditNoteRefund refund = refundRepository.findByIdAndTenantId(refundId, tenantId)
                .orElseThrow(() -> new CommonException("Credit note refund not found", HttpStatus.NOT_FOUND));

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new BadRequestException("Refund is already " + refund.getStatus());
        }

        refund.setStatus(RefundStatus.CLEARED);
        refundRepository.save(refund);

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .message("Credit note refund marked as cleared")
                .build();
    }

    // QUERIES
    @Override
    @Transactional(readOnly = true)
    public CreditNoteDto getCreditNote(Long creditNoteId) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        CreditNote cn = creditNoteRepository.findByIdAndTenantId(creditNoteId, tenantId)
                .orElseThrow(() -> new CommonException("Credit Note not found", HttpStatus.NOT_FOUND));
        return mapToDto(cn, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreditNoteDto> getCreditNotesByCustomer(Long customerId) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        return creditNoteRepository.findByCustomerIdAndTenantIdOrderByIssueDateDesc(customerId, tenantId)
                .stream()
                .map(cn -> mapToDto(cn, false))
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

    private CreditNoteDto mapToDto(CreditNote cn, boolean includeDetails) {
        CreditNoteDto dto = CreditNoteDto.builder()
                .id(cn.getId())
                .creditNoteNumber(cn.getCreditNoteNumber())
                .customerId(cn.getCustomerId())
                .sourceReturnId(cn.getSourceReturnId())
                .issueDate(cn.getIssueDate())
                .amount(cn.getAmount())
                .availableBalance(cn.getAvailableBalance())
                .status(cn.getStatus())
                .remarks(cn.getRemarks())
                .build();

        if (includeDetails) {
            dto.setUtilizations(cn.getUtilizations().stream()
                    .map(u -> CreditNoteDto.UtilizationItem.builder()
                            .id(u.getId())
                            .invoiceId(u.getInvoiceId())
                            .utilizedAmount(u.getUtilizedAmount())
                            .utilizationDate(u.getUtilizationDate())
                            .status(u.getStatus())
                            .build())
                    .collect(Collectors.toList()));

            dto.setRefunds(cn.getRefunds().stream()
                    .map(r -> CreditNoteDto.RefundItem.builder()
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
