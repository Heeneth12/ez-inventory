package com.ezh.Inventory.stock.entity;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "stock_ledger")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockLedger extends CommonSerializable {

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "transaction_type", columnDefinition = "movement_type") // IN / OUT
    private MovementType transactionType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "reference_type", columnDefinition = "reference_type") // GRN / SALE / TRANSFER / RETURN
    private ReferenceType referenceType;

    @Column(name = "reference_id")  //GRN_ID / SALE_ID ...
    private Long referenceId;

    @Column(name = "before_qty", nullable = false)
    private Integer beforeQty;

    @Column(name = "after_qty", nullable = false)
    private Integer afterQty;

    @Column(name = "unit_price", precision = 18, scale = 2)
    private BigDecimal unitPrice; // Price per item (Purchase price or Selling price)

    @Column(name = "total_value", precision = 18, scale = 2)
    private BigDecimal totalValue; // quantity * unit_price
}
