package com.ezh.Inventory.payment.service;

import com.ezh.Inventory.payment.dto.*;
import com.ezh.Inventory.payment.entity.Payment;
import com.ezh.Inventory.payment.entity.PaymentAllocation;
import com.ezh.Inventory.payment.repository.CreditNoteRepository;
import com.ezh.Inventory.payment.repository.CustomerAdvanceRepository;
import com.ezh.Inventory.payment.repository.PaymentAllocationRepository;
import com.ezh.Inventory.payment.repository.PaymentRepository;
import com.ezh.Inventory.sales.invoice.entity.Invoice;
import com.ezh.Inventory.sales.invoice.entity.InvoicePaymentStatus;
import com.ezh.Inventory.sales.invoice.repository.InvoiceRepository;
import com.ezh.Inventory.payment.entity.enums.PaymentStatus;
import com.ezh.Inventory.utils.UserContextUtil;
import com.ezh.Inventory.utils.common.*;
import com.ezh.Inventory.utils.common.client.AuthServiceClient;
import com.ezh.Inventory.utils.common.dto.UserMiniDto;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerAdvanceRepository advanceRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final AuthServiceClient authServiceClient;

    // RECORD PAYMENT — pure invoice revenue, no excess allowed
    @Override
    @Transactional
    public CommonResponse<?> recordPayment(PaymentCreateDto dto) throws CommonException {
        Long tenantId = (dto.getTenantId() != null) ? dto.getTenantId() : UserContextUtil.getTenantIdOrThrow();

        if (dto.getAllocations() == null || dto.getAllocations().isEmpty()) {
            throw new BadRequestException("At least one invoice allocation is required. For advance payments use POST /v1/advance");
        }

        // Validate: sum of allocations must equal payment amount
        BigDecimal allocationSum = dto.getAllocations().stream()
                .map(PaymentAllocationDto::getAmountToPay)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (allocationSum.compareTo(dto.getTotalAmount()) != 0) {
            throw new BadRequestException(
                    "Allocation sum (" + allocationSum + ") must exactly equal payment amount (" + dto.getTotalAmount() + ")");
        }

        Payment payment = Payment.builder()
                .tenantId(tenantId)
                .paymentNumber(DocumentNumberUtil.generate(DocPrefix.PAY))
                .customerId(dto.getCustomerId())
                .paymentDate(new Date())
                .amount(dto.getTotalAmount())
                .paymentMethod(dto.getPaymentMethod())
                .status(PaymentStatus.CONFIRMED)
                .referenceNumber(dto.getReferenceNumber())
                .remarks(dto.getRemarks())
                .allocations(new ArrayList<>())
                .build();

        payment = paymentRepository.save(payment);

        for (PaymentAllocationDto allocDto : dto.getAllocations()) {
            Invoice invoice = invoiceRepository.findByIdAndTenantId(allocDto.getInvoiceId(), tenantId)
                    .orElseThrow(() -> new CommonException("Invoice " + allocDto.getInvoiceId() + " not found",
                            HttpStatus.BAD_REQUEST));

            if (allocDto.getAmountToPay().compareTo(invoice.getBalance()) > 0) {
                throw new BadRequestException("Amount " + allocDto.getAmountToPay()
                        + " exceeds balance for invoice " + invoice.getInvoiceNumber());
            }

            PaymentAllocation allocation = PaymentAllocation.builder()
                    .tenantId(tenantId)
                    .payment(payment)
                    .invoiceId(allocDto.getInvoiceId())
                    .allocatedAmount(allocDto.getAmountToPay())
                    .allocationDate(new Date())
                    .build();

            allocationRepository.save(allocation);
            updateInvoiceBalance(invoice, allocDto.getAmountToPay());
        }

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .id(payment.getId().toString())
                .message("Payment recorded successfully")
                .build();
    }

    // QUERIES
    @Override
    @Transactional(readOnly = true)
    public Page<PaymentDto> getAllPayments(PaymentFilter filter, Integer page, Integer size) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        String statusStr = (filter.getStatus() != null) ? filter.getStatus().name() : null;

        Page<Payment> payments = paymentRepository.getAllPayments(
                tenantId, filter.getId(), filter.getCustomerId(),
                statusStr, filter.getPaymentMethod(), filter.getPaymentNumber(), null, pageable);

        List<Long> customerIds = payments.getContent().stream()
                .map(Payment::getCustomerId).distinct().toList();

        Map<Long, UserMiniDto> customerMap = customerIds.isEmpty()
                ? new HashMap<>()
                : authServiceClient.getBulkUserDetails(customerIds);

        return payments.map(p -> mapToDto(p, customerMap, false));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDto getPayment(Long paymentId) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        Payment payment = paymentRepository.findByIdAndTenantId(paymentId, tenantId)
                .orElseThrow(() -> new CommonException("Payment not found", HttpStatus.NOT_FOUND));
        Map<Long, UserMiniDto> customerMap = authServiceClient.getBulkUserDetails(List.of(payment.getCustomerId()));
        return mapToDto(payment, customerMap, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoicePaymentHistoryDto> getPaymentsByInvoiceId(Long invoiceId) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        return allocationRepository
                .findByInvoiceIdAndTenantIdOrderByAllocationDateDesc(invoiceId, tenantId)
                .stream()
                .map(alloc -> {
                    Payment p = alloc.getPayment();
                    return InvoicePaymentHistoryDto.builder()
                            .id(alloc.getId())
                            .paymentId(p.getId())
                            .paymentNumber(p.getPaymentNumber())
                            .paymentDate(p.getPaymentDate())
                            .amount(alloc.getAllocatedAmount())
                            .method(p.getPaymentMethod())
                            .referenceNumber(p.getReferenceNumber())
                            .remarks(p.getRemarks())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InvoicePaymentSummaryDto getInvoicePaymentSummary(Long invoiceId) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Invoice invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new CommonException("Invoice not found", HttpStatus.NOT_FOUND));

        Map<Long, UserMiniDto> customerMap = authServiceClient.getBulkUserDetails(List.of(invoice.getCustomerId()));

        List<InvoicePaymentHistoryDto> history = allocationRepository
                .findByInvoiceIdAndTenantIdOrderByAllocationDateDesc(invoiceId, tenantId)
                .stream()
                .map(alloc -> {
                    Payment p = alloc.getPayment();
                    return InvoicePaymentHistoryDto.builder()
                            .id(alloc.getId())
                            .paymentId(p.getId())
                            .paymentNumber(p.getPaymentNumber())
                            .paymentDate(p.getPaymentDate())
                            .amount(alloc.getAllocatedAmount())
                            .method(p.getPaymentMethod())
                            .referenceNumber(p.getReferenceNumber())
                            .remarks(p.getRemarks())
                            .build();
                })
                .collect(Collectors.toList());

        return InvoicePaymentSummaryDto.builder()
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .customerId(customerMap.get(invoice.getCustomerId()).getId())
                .customerName(customerMap.get(invoice.getCustomerId()).getName())
                .invoiceDate(invoice.getInvoiceDate())
                .status(invoice.getStatus())
                .grandTotal(invoice.getGrandTotal())
                .totalPaid(invoice.getAmountPaid())
                .balanceDue(invoice.getBalance())
                .paymentHistory(history)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerFinancialSummaryDto getCustomerFinancialSummary(Long customerId) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        BigDecimal totalDue = invoiceRepository.getTotalBalanceByCustomer(customerId, tenantId);
        BigDecimal advanceBalance = advanceRepository.getTotalAvailableBalance(customerId, tenantId);
        BigDecimal creditNoteBalance = creditNoteRepository.getTotalAvailableBalance(customerId, tenantId);

        return CustomerFinancialSummaryDto.builder()
                .customerId(customerId)
                .totalOutstandingAmount(totalDue != null ? totalDue : BigDecimal.ZERO)
                .advanceBalance(advanceBalance != null ? advanceBalance : BigDecimal.ZERO)
                .creditNoteBalance(creditNoteBalance != null ? creditNoteBalance : BigDecimal.ZERO)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStats getStats(CommonFilter filter) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        return paymentRepository.getPaymentStats(tenantId);
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

    private PaymentDto mapToDto(Payment payment, Map<Long, UserMiniDto> customerMap, boolean includeAllocations) {
        UserMiniDto user = customerMap.getOrDefault(payment.getCustomerId(), new UserMiniDto());

        List<PaymentDto.AllocationItem> allocations = new ArrayList<>();
        if (includeAllocations && payment.getAllocations() != null) {
            allocations = payment.getAllocations().stream()
                    .map(a -> PaymentDto.AllocationItem.builder()
                            .invoiceId(a.getInvoiceId())
                            .allocatedAmount(a.getAllocatedAmount())
                            .allocationDate(a.getAllocationDate())
                            .build())
                    .collect(Collectors.toList());
        }

        return PaymentDto.builder()
                .id(payment.getId())
                .tenantId(payment.getTenantId())
                .paymentNumber(payment.getPaymentNumber())
                .customerId(payment.getCustomerId())
                .customerName(user.getName())
                .contactMini(user)
                .paymentDate(payment.getPaymentDate())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .referenceNumber(payment.getReferenceNumber())
                .bankName(payment.getBankName())
                .remarks(payment.getRemarks())
                .allocations(allocations)
                .build();
    }
}
