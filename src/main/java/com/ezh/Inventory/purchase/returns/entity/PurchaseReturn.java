package com.ezh.Inventory.purchase.returns.entity;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "purchase_return")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseReturn extends CommonSerializable {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "goods_receipt_id")
    private Long goodsReceiptId; // Optional: Link to original receipt

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "return_date")
    private Long returnDate;

    @Column(name = "reason")
    private String reason;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", columnDefinition = "return_status")
    private ReturnStatus status; // DRAFT, COMPLETED
}
