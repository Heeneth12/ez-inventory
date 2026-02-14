package com.ezh.Inventory.purchase.grn.entity;

import com.ezh.Inventory.purchase.po.entity.PurchaseOrder;
import com.ezh.Inventory.purchase.returns.entity.PurchaseReturn;
import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

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
    @Column(name = "grn_status", length = 50)
    private GrnStatus grnStatus;
    // PENDING_QA, RECEIVED (Stock Increases here), CANCELLED

    // Relationships for JOIN FETCH optimization
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", insertable = false, updatable = false)
    private PurchaseOrder purchaseOrder;

    @OneToMany(mappedBy = "goodsReceiptId", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<GoodsReceiptItem> items = new HashSet<>();

    @OneToMany(mappedBy = "goodsReceiptId", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<PurchaseReturn> purchaseReturns = new HashSet<>();
}