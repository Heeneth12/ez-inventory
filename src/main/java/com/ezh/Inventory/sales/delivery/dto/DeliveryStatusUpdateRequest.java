package com.ezh.Inventory.sales.delivery.dto;

import com.ezh.Inventory.sales.delivery.entity.ShipmentStatus;
import lombok.Data;

import java.util.Date;

@Data
public class DeliveryStatusUpdateRequest {
    private ShipmentStatus status;
    private String reason;
    private Date scheduledDate;
}
