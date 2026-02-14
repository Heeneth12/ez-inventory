package com.ezh.Inventory.purchase.po.entity;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrder extends CommonSerializable {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "purchase_request_id")
    private Long purchaseRequestId;

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber; // e.g., PO-2023-001

    @Column(name = "order_date")
    private Long orderDate;

    @Column(name = "expected_delivery_date")
    private Long expectedDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "po_status", length = 50)
    private PoStatus poStatus;

    @Column(name = "flat_discount")
    private BigDecimal flatDiscount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "flat_tax")
    private BigDecimal flatTax = BigDecimal.ZERO;

    @Column(name = "total_discount")
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_tax")
    private BigDecimal totalTax = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "grand_total", precision = 18, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "notes")
    private String notes;
}