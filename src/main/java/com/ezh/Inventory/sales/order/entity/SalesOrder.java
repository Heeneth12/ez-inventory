package com.ezh.Inventory.sales.order.entity;

import com.ezh.Inventory.utils.AbstractFinancialHeader;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "sales_order")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrder extends AbstractFinancialHeader {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "order_number", nullable = false, unique = true, length = 40)
    private String orderNumber;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "order_date", nullable = false)
    private Date orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private SalesOrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 50)
    private SalesOrderSource source;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalesOrderItem> items = new ArrayList<>();

    // The fields: itemGrossTotal, itemTotalDiscount, itemTotalTax, flatDiscountRate,
    // flatTaxRate, and grandTotal are all inherited from AbstractFinancialHeader!
}