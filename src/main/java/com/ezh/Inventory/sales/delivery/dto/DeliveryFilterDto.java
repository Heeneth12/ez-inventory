package com.ezh.Inventory.sales.delivery.dto;

import com.ezh.Inventory.sales.delivery.entity.ShipmentStatus;
import com.ezh.Inventory.sales.delivery.entity.ShipmentType;
import com.ezh.Inventory.utils.common.CommonFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;


@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryFilterDto extends CommonFilter {
    private Long deliveryId;
    private Long customerId;
    private Long invoiceId;
    private String deliveryNumber;
    private List<ShipmentType> shipmentTypes;
    private List<ShipmentStatus> shipmentStatuses;
}
