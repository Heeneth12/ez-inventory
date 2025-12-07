package com.ezh.Inventory.purchase.returns.entity;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_return_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseReturnItem extends CommonSerializable {

    @Column(name = "purchase_return_id", nullable = false)
    private Long purchaseReturnId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "batch_number", nullable = false)
    private String batchNumber;

    @Column(name = "return_qty", nullable = false)
    private Integer returnQty;

    @Column(name = "refund_price", precision = 18, scale = 2)
    private BigDecimal refundPrice; // Usually same as PO Unit Price
}