package com.ezh.Inventory.sales.payment.service;

import com.ezh.Inventory.contacts.dto.ContactMiniDto;
import com.ezh.Inventory.contacts.entiry.Contact;
import com.ezh.Inventory.contacts.repository.ContactRepository;
import com.ezh.Inventory.sales.invoice.dto.InvoiceMiniDto;
import com.ezh.Inventory.sales.invoice.entity.Invoice;
import com.ezh.Inventory.sales.invoice.entity.InvoicePaymentStatus;
import com.ezh.Inventory.sales.invoice.repository.InvoiceRepository;
import com.ezh.Inventory.sales.payment.dto.*;
import com.ezh.Inventory.sales.payment.entity.Payment;
import com.ezh.Inventory.sales.payment.entity.PaymentAllocation;
import com.ezh.Inventory.sales.payment.entity.PaymentMethod;
import com.ezh.Inventory.sales.payment.entity.PaymentStatus;
import com.ezh.Inventory.sales.payment.repository.PaymentAllocationRepository;
import com.ezh.Inventory.sales.payment.repository.PaymentRepository;
import com.ezh.Inventory.utils.UserContextUtil;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final InvoiceRepository invoiceRepository;
    private final ContactRepository contactRepository;

    @Override
    @Transactional
    public CommonResponse recordPayment(PaymentCreateDto dto) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Contact customer = contactRepository.findByIdAndTenantId(dto.getCustomerId(), tenantId)
                .orElseThrow(() -> new CommonException("Customer not found", HttpStatus.NOT_FOUND));

        // 1. Create Payment Header (Source of Funds)
        Payment payment = Payment.builder()
                .tenantId(tenantId)
                .paymentNumber("PAY-" + System.currentTimeMillis())
                .customer(customer)
                .paymentDate(new Date())
                .amount(dto.getTotalAmount())
                .status(PaymentStatus.COMPLETED)
                .paymentMethod(dto.getPaymentMethod())
                .referenceNumber(dto.getReferenceNumber())
                .remarks(dto.getRemarks())
                .allocatedAmount(BigDecimal.ZERO)
                .unallocatedAmount(dto.getTotalAmount()) // Initially all unallocated
                .allocations(new ArrayList<>())
                .build();

        // Save strictly to generate ID
        payment = paymentRepository.save(payment);

        // 2. Process Allocations (If any)
        if (dto.getAllocations() != null && !dto.getAllocations().isEmpty()) {
            processAllocations(payment, dto.getAllocations(), tenantId);
        }

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .id(payment.getId().toString())
                .message("Payment Recorded Successfully")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoicePaymentSummaryDto getInvoicePaymentSummary(Long invoiceId) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        // 1. Fetch Invoice
        Invoice invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new CommonException("Invoice not found", HttpStatus.NOT_FOUND));

        // 2. Fetch History
        List<PaymentAllocation> history = allocationRepository
                .findByInvoiceIdAndTenantIdOrderByAllocationDateDesc(invoiceId, tenantId);

        // 3. Map History Items (FIXED: Accessing Parent Payment for details)
        List<InvoicePaymentHistoryDto> historyDtos = history.stream()
                .map(alloc -> {
                    Payment parentPayment = alloc.getPayment(); // Get the header
                    return InvoicePaymentHistoryDto.builder()
                            .id(alloc.getId())
                            .paymentId(parentPayment.getId())
                            .paymentNumber(parentPayment.getPaymentNumber())
                            .paymentDate(parentPayment.getPaymentDate()) // Use Receipt Date
                            .amount(alloc.getAllocatedAmount())
                            // FIX: Get details from Parent Payment, not Allocation
                            .method(parentPayment.getPaymentMethod())
                            .referenceNumber(parentPayment.getReferenceNumber())
                            .remarks(parentPayment.getRemarks())
                            .build();
                })
                .collect(Collectors.toList());

        // 4. Build Summary
        return InvoicePaymentSummaryDto.builder()
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .customerId(invoice.getCustomer().getId())
                .customerName(invoice.getCustomer().getName())
                .invoiceDate(invoice.getInvoiceDate())
                .status(invoice.getStatus())
                .grandTotal(invoice.getGrandTotal())
                .totalPaid(invoice.getAmountPaid())
                .balanceDue(invoice.getBalance())
                .paymentHistory(historyDtos)
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public List<InvoicePaymentHistoryDto> getPaymentsByInvoiceId(Long invoiceId) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        // 1. Fetch Allocations
        List<PaymentAllocation> allocations = allocationRepository
                .findByInvoiceIdAndTenantIdOrderByAllocationDateDesc(invoiceId, tenantId);

        // 2. Map to DTO (FIXED)
        return allocations.stream()
                .map(alloc -> {
                    Payment parentPayment = alloc.getPayment(); // Get the header
                    return InvoicePaymentHistoryDto.builder()
                            .id(alloc.getId())
                            .paymentId(parentPayment.getId())
                            .paymentNumber(parentPayment.getPaymentNumber())
                            .paymentDate(parentPayment.getPaymentDate())
                            .amount(alloc.getAllocatedAmount())
                            // FIX: Get details from Parent Payment
                            .method(parentPayment.getPaymentMethod())
                            .referenceNumber(parentPayment.getReferenceNumber())
                            .remarks(parentPayment.getRemarks())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentDto> getAllPayments(PaymentFilter filter, Integer page, Integer size) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<Payment> payments = paymentRepository.getAllPayments(tenantId,
                filter.getId(), filter.getCustomerId(), filter.getStatus(),
                filter.getPaymentMethod(), filter.getPaymentNumber(),
                pageable);

        return payments.map(p -> mapToDto(p, false));
    }


    @Override
    @Transactional(readOnly = true)
    public PaymentDto getPayment(Long PaymentId) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Payment payment = paymentRepository.findByIdAndTenantId(PaymentId,tenantId)
                .orElseThrow(() -> new CommonException("", HttpStatus.NOT_FOUND));


        return mapToDto(payment, true);
    }

    @Override
    @Transactional()
    public CommonResponse createCreditNote(Contact customer, BigDecimal amount, String returnRefNumber) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        String creditNoteNumber = "CN-" + System.currentTimeMillis();

        Payment creditNote = Payment.builder()
                .tenantId(tenantId)
                .paymentNumber(creditNoteNumber)
                .customer(customer)
                .paymentDate(new Date())
                .amount(amount)
                // Status is RECEIVED because the company technically "received" the value back
                .status(PaymentStatus.RECEIVED)
                .paymentMethod(PaymentMethod.CREDIT_NOTE)
                .referenceNumber(returnRefNumber)
                .remarks("Auto-generated Credit Note for Sales Return: " + returnRefNumber)
                // CRITICAL: The money is fully available (Unallocated)
                // This allows the user to apply this amount to other invoices later
                .allocatedAmount(BigDecimal.ZERO)
                .unallocatedAmount(amount)
                .build();
        // 3. Save and Return
        paymentRepository.save(creditNote);

        return CommonResponse
                .builder()
                .status(Status.SUCCESS)
                .message("Successfully created credit note")
                .build();
    }

    @Override
    @Transactional
    public CommonResponse<?> applyWalletToInvoice(WalletPayDto walletPayDto) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        BigDecimal amountToApply = walletPayDto.getAmount();

        // 1. Fetch all available credits for this customer (Oldest First - FIFO)
        List<Payment> availableCredits = paymentRepository
                .findByCustomerIdAndTenantIdAndUnallocatedAmountGreaterThanOrderByPaymentDateAsc(
                        walletPayDto.getCustomerId(), tenantId, BigDecimal.ZERO);

        // 2. Validate total available balance across all records
        BigDecimal totalAvailable = availableCredits.stream()
                .map(Payment::getUnallocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAvailable.compareTo(amountToApply) < 0) {
            throw new BadRequestException("Insufficient total wallet balance. Available: " + totalAvailable);
        }

        // 3. Consume credits across multiple payment records if necessary
        for (Payment payment : availableCredits) {
            if (amountToApply.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal canTake = payment.getUnallocatedAmount().min(amountToApply);

            // Prepare allocation for this specific payment record
            List<PaymentAllocationDto> allocations = List.of(
                    new PaymentAllocationDto(walletPayDto.getInvoiceId(), canTake)
            );

            // Use your existing logic to update balances and create PaymentAllocation entities
            processAllocations(payment, allocations, tenantId);

            amountToApply = amountToApply.subtract(canTake);
        }

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .message("Wallet balance applied to invoice successfully using FIFO")
                .build();
    }

    @Override
    @Transactional
    public CommonResponse<?> refundUnallocatedAmount(Long paymentId, BigDecimal refundAmount) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Payment payment = paymentRepository.findByIdAndTenantId(paymentId, tenantId)
                .orElseThrow(() -> new CommonException("Payment record not found", HttpStatus.NOT_FOUND));

        if (payment.getUnallocatedAmount().compareTo(refundAmount) < 0) {
            throw new BadRequestException("Refund amount exceeds available unallocated balance");
        }

        // Reduce unallocated amount and add a remark
        payment.setUnallocatedAmount(payment.getUnallocatedAmount().subtract(refundAmount));
        payment.setRemarks(payment.getRemarks() + " | Refunded: " + refundAmount);

        paymentRepository.save(payment);

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .message("Refund processed successfully")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerFinancialSummaryDto getCustomerFinancialSummary(Long customerId)  throws CommonException{
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        // 1. Calculate Total Due (Sum of all unpaid/partial invoice balances)
        BigDecimal totalDue = invoiceRepository.getTotalBalanceByCustomer(customerId, tenantId);

        // 2. Calculate Wallet Balance (Sum of all unallocated payment amounts)
        BigDecimal walletBalance = paymentRepository.getTotalUnallocatedByCustomer(customerId, tenantId);

        return CustomerFinancialSummaryDto.builder()
                .customerId(customerId)
                .totalOutstandingAmount(totalDue != null ? totalDue : BigDecimal.ZERO)
                .walletBalance(walletBalance != null ? walletBalance : BigDecimal.ZERO)
                .build();
    }


    @Override
    @Transactional
    public CommonResponse<?> addMoneyToWallet(WalletAddDto dto) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Contact customer = contactRepository.findByIdAndTenantId(dto.getCustomerId(), tenantId)
                .orElseThrow(() -> new CommonException("Customer not found", HttpStatus.NOT_FOUND));

        // Create Payment as an "Advance"
        Payment payment = Payment.builder()
                .tenantId(tenantId)
                .paymentNumber("ADV-" + System.currentTimeMillis())
                .customer(customer)
                .paymentDate(new Date())
                .amount(dto.getAmount())
                .status(PaymentStatus.COMPLETED)
                .paymentMethod(dto.getPaymentMethod())
                .referenceNumber(dto.getReferenceNumber())
                .remarks(dto.getRemarks())
                .allocatedAmount(BigDecimal.ZERO)
                .unallocatedAmount(dto.getAmount()) // All money goes to wallet
                .build();

        paymentRepository.save(payment);

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .id(payment.getId().toString())
                .message("Advance Payment Added to Wallet")
                .build();
    }


    private void processAllocations(Payment payment, List<PaymentAllocationDto> allocDtos, Long tenantId) {
        BigDecimal totalAllocatedNow = BigDecimal.ZERO;

        for (PaymentAllocationDto allocDto : allocDtos) {
            Invoice invoice = invoiceRepository.findByIdAndTenantId(allocDto.getInvoiceId(), tenantId)
                    .orElseThrow(() -> new CommonException("Invoice " + allocDto.getInvoiceId() + " not found", HttpStatus.BAD_REQUEST));

            // A. Validate: Don't overpay the invoice
            if (allocDto.getAmountToPay().compareTo(invoice.getBalance()) > 0) {
                throw new CommonException("Amount " + allocDto.getAmountToPay() + " exceeds balance for Invoice " + invoice.getInvoiceNumber(), HttpStatus.BAD_REQUEST);
            }

            // B. Create Allocation Record
            PaymentAllocation allocation = PaymentAllocation.builder()
                    .tenantId(tenantId)
                    .payment(payment)
                    .invoice(invoice)
                    .allocatedAmount(allocDto.getAmountToPay())
                    .allocationDate(new Date())
                    .build();

            allocationRepository.save(allocation);

            // C. Update Invoice Balance & Status
            updateInvoiceBalance(invoice, allocDto.getAmountToPay());

            totalAllocatedNow = totalAllocatedNow.add(allocDto.getAmountToPay());
        }

        // D. Update Payment Header Totals
        payment.setAllocatedAmount(payment.getAllocatedAmount().add(totalAllocatedNow));
        payment.setUnallocatedAmount(payment.getAmount().subtract(payment.getAllocatedAmount()));

        // Validation: Did user try to allocate more than the check amount?
        if (payment.getUnallocatedAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Allocation sum exceeds Total Payment Amount");
        }

        paymentRepository.save(payment);
    }

    private void updateInvoiceBalance(Invoice invoice, BigDecimal paidAmount) {
        BigDecimal newPaid = invoice.getAmountPaid().add(paidAmount);
        BigDecimal newBalance = invoice.getGrandTotal().subtract(newPaid);

        invoice.setAmountPaid(newPaid);
        invoice.setBalance(newBalance);

        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setPaymentStatus(InvoicePaymentStatus.PAID);
        } else {
            invoice.setPaymentStatus(InvoicePaymentStatus.PARTIALLY_PAID);
        }
        invoiceRepository.save(invoice);
    }

    private PaymentDto mapToDto(Payment payment, Boolean subDetails) {
        if (payment == null) return null;

        // 1. Map the allocations to the
        List<InvoiceMiniDto> invoiceDetails = new ArrayList<>();

        if (payment.getAllocations() != null && subDetails) {
            invoiceDetails = payment.getAllocations().stream()
                    .map(allocation -> {
                        // Get the invoice entity from the allocation
                        var invoice = allocation.getInvoice();
                        return InvoiceMiniDto.builder()
                                .id(invoice.getId())
                                .invoiceNumber(invoice.getInvoiceNumber())
                                .invoiceDate(invoice.getInvoiceDate())
                                .amountPaid(allocation.getAllocatedAmount())
                                .grandTotal(invoice.getGrandTotal())
                                .balance(invoice.getBalance())
                                .status(invoice.getStatus())
                                .paymentStatus(invoice.getPaymentStatus())
                                .customerId(invoice.getCustomer().getId())
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        return PaymentDto.builder()
                .id(payment.getId())
                .tenantId(payment.getTenantId())
                .paymentNumber(payment.getPaymentNumber())
                .customerId(payment.getCustomer().getId())
                .customerName(payment.getCustomer().getName())
                .paymentDate(payment.getPaymentDate())
                .contactMini(new ContactMiniDto(payment.getCustomer()))
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .referenceNumber(payment.getReferenceNumber())
                .bankName(payment.getBankName())
                .remarks(payment.getRemarks())
                .invoices(invoiceDetails)
                .allocatedAmount(payment.getAllocatedAmount())
                .unallocatedAmount(payment.getUnallocatedAmount())
                .build();
    }
}