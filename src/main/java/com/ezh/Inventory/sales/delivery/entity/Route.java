package com.ezh.Inventory.sales.delivery.entity;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "delivery_route")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route  extends CommonSerializable {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "route_number", unique = true, nullable = false)
    private String routeNumber; // ROUTE-001

    @Column(name = "area_name")
    private String areaName;

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "vehicle_number")
    private String vehicleNumber;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "route_status")
    private RouteStatus status; // CREATED, IN_TRANSIT, COMPLETED

    @OneToMany(mappedBy = "route")
    private List<Delivery> deliveries = new ArrayList<>();

    @Column(name = "start_date")
    private Date startDate;
}
