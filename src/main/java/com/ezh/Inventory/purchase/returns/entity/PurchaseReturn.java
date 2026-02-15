package com.ezh.Inventory.purchase.returns.entity;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;

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

    @Column(name = "pr_number", unique = true)
    private String prNumber; // e.g., PR-2023-999

    @Column(name = "goods_receipt_id")
    private Long goodsReceiptId; // Optional: Link to original receipt

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "return_date")
    private Long returnDate;

    @Column(name = "reason")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "pr_status", length = 50)
    private ReturnStatus prStatus;

    @OneToMany(mappedBy = "purchaseReturnId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PurchaseReturnItem> purchaseReturnItems;
}
