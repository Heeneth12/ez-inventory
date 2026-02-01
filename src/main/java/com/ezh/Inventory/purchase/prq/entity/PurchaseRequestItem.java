package com.ezh.Inventory.purchase.prq.entity;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_request_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestItem extends CommonSerializable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_request_id", nullable = false)
    private PurchaseRequest purchaseRequest;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "requested_qty", nullable = false)
    private Integer requestedQty;

    @Column(name = "estimated_unit_price", precision = 18, scale = 2)
    private BigDecimal estimatedUnitPrice;

    @Column(name = "line_total", precision = 18, scale = 2)
    private BigDecimal lineTotal;
}