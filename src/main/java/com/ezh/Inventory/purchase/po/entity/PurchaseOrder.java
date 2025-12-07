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

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "supplier_name", nullable = false)
    private String supplierName;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber; // e.g., PO-2023-001

    @Column(name = "order_date")
    private Long orderDate;

    @Column(name = "expected_delivery_date")
    private Long expectedDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PoStatus status;
    // DRAFT, ISSUED, PARTIALLY_RECEIVED, COMPLETED, CANCELLED

    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "notes")
    private String notes;
}