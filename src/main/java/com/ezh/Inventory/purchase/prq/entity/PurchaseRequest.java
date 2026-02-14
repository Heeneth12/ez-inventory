package com.ezh.Inventory.purchase.prq.entity;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_request")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequest extends CommonSerializable {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "requested_by_user_id")
    private Long requestedBy;

    @Column(name = "department")
    private String department;

    @Column(name = "prq_number", unique = true, nullable = false)
    private String prqNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PrqStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source" ,nullable = false)
    private PrqSource source;

    @Column(name = "total_estimated_amount", precision = 18, scale = 2)
    private BigDecimal totalEstimatedAmount;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_by")
    protected Long createdBy;

    // One-to-Many Relationship
    @OneToMany(mappedBy = "purchaseRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseRequestItem> items = new ArrayList<>();

    // Helper method to maintain bidirectional sync
    public void addItem(PurchaseRequestItem item) {
        items.add(item);
        item.setPurchaseRequest(this);
    }
}