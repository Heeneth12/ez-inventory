package com.ezh.Inventory.purchase.grn.entity;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "goods_receipt_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptItem extends CommonSerializable {

    @Column(name = "goods_receipt_id", nullable = false)
    private Long goodsReceiptId;

    @Column(name = "po_item_id")
    private Long poItemId; // Link to specific line in PO

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "received_qty", nullable = false)
    private Integer receivedQty; // Physical count

    @Column(name = "accepted_qty", nullable = false)
    private Integer acceptedQty; // received - rejected

    @Column(name = "rejected_qty", nullable = false)
    private Integer rejectedQty; // Damaged items sent back immediately

    @Column(name = "batch_number")
    private String batchNumber; // Critical for expiry tracking

    @Column(name = "expiry_date")
    private Long expiryDate;
}
