package com.ezh.Inventory.purchase.grn.entity;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "goods_receipt")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceipt extends CommonSerializable {

    @Column(name = "purchase_order_id", nullable = false)
    private Long purchaseOrderId; // Link back to PO

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "grn_number", unique = true)
    private String grnNumber; // e.g., GRN-2023-999

    @Column(name = "received_date")
    private Long receivedDate;

    @Column(name = "supplier_invoice_no")
    private String supplierInvoiceNo; // The paper bill number from supplier

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", columnDefinition = "grn_status")
    private GrnStatus status;
    // PENDING_QA, APPROVED (Stock Increases here), REJECTED
}