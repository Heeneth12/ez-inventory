package com.ezh.Inventory.stock.entity;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "stock_adjustment_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAdjustmentItem extends CommonSerializable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjustment_id", nullable = false)
    private StockAdjustment stockAdjustment;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "system_qty", nullable = false)
    private Integer systemQty; // Expected qty in DB at that moment

    @Column(name = "counted_qty", nullable = false)
    private Integer countedQty; // Actual physical qty

    @Column(name = "difference_qty", nullable = false)
    private Integer differenceQty; // Calculated: counted - system

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "reason_type", columnDefinition = "adjustment_type")
    private AdjustmentType reasonType;
}
