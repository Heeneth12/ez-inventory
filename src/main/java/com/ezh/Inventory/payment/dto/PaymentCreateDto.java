package com.ezh.Inventory.payment.dto;

import com.ezh.Inventory.payment.entity.enums.PaymentMethod;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateDto {
    private Long customerId;
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private String referenceNumber;
    private String remarks;
    // Optional:Empty list = Advance Payment
    private List<PaymentAllocationDto> allocations;
    /**
     * When set, overrides {@code UserContextUtil.getTenantIdOrThrow()}.
     * Used by webhook handlers that run without a JWT security context.
     */
    private Long tenantId;
}