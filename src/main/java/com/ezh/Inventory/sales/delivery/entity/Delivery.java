package com.ezh.Inventory.sales.delivery.entity;

import com.ezh.Inventory.sales.invoice.entity.Invoice;
import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Entity
@Table(name = "delivery")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Delivery extends CommonSerializable {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "delivery_number", unique = true, nullable = false)
    private String deliveryNumber;  // DEV-2025-001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private ShipmentType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ShipmentStatus status;

    @Column(name = "employee_id")
    private Long deliveryPerson;

    @Column(name = "scheduled_date")
    private Date scheduledDate;

    @Column(name = "shipped_date")
    private Date shippedDate;

    @Column(name = "delivered_date")
    private Date deliveredDate;

    @Column(name = "delivery_address", length = 500)
    private String deliveryAddress;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "remarks")
    private String remarks;

    @Builder.Default
    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeliveryItem> items = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;
}
