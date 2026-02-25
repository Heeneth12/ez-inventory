package com.ezh.Inventory.sales.order.entity;

import com.ezh.Inventory.utils.AbstractFinancialLine;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sales_order_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrderItem extends AbstractFinancialLine {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "ordered_qty", nullable = false)
    private Integer orderedQty;

    @Column(name = "invoiced_qty", nullable = false)
    private Integer invoicedQty = 0;

    // The fields: unitPrice, discountRate, discountAmount, taxRate, taxAmount,
    // quantity, and lineTotal are all inherited from AbstractFinancialLine!
}
