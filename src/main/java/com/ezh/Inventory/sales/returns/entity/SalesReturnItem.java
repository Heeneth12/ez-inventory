package com.ezh.Inventory.sales.returns.entity;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "sales_return_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReturnItem extends CommonSerializable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_return_id", nullable = false)
    private SalesReturn salesReturn;

    @JoinColumn(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "reason")
    private String reason; // "Damaged", "Wrong Item", etc.
}