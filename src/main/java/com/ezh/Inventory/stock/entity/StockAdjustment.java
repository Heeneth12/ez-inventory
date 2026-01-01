package com.ezh.Inventory.stock.entity;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Entity
@Table(name = "stock_adjustment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAdjustment extends CommonSerializable {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "adjustment_number", nullable = false, unique = true)
    private String adjustmentNumber; // e.g., ADJ-2025-0001

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "adjustment_date", nullable = false)
    private Date adjustmentDate;

    @Column(name = "reason_type", nullable = false)
    private AdjustmentType reasonType; // DAMAGE, EXPIRED

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", columnDefinition = "adjustment_status")
    private AdjustmentStatus status; //DRAFT (Counting in progress), APPROVED (Stock updated), CANCELLED

    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "remarks")
    private String remarks;

    @Builder.Default
    @OneToMany(mappedBy = "stockAdjustment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockAdjustmentItem> adjustmentItems = new ArrayList<>();
}
