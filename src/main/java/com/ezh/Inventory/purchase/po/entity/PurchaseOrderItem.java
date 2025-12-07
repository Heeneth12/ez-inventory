package com.ezh.Inventory.purchase.po.entity;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItem extends CommonSerializable {

    @Column(name = "purchase_order_id", nullable = false)
    private Long purchaseOrderId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "ordered_qty", nullable = false)
    private Integer orderedQty;

    @Column(name = "received_qty", nullable = false)
    private Integer receivedQty = 0; // Tracks how many arrived so far

    @Column(name = "unit_price", precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", precision = 18, scale = 2)
    private BigDecimal lineTotal; // orderedQty * unitPrice
}
