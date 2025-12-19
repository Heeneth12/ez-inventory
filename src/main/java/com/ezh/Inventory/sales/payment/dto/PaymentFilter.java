package com.ezh.Inventory.sales.payment.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentFilter {
    private Long id;
    private Long customerId;
    private String status;
    private String paymentMethod;
    private String paymentNumber;
}
