package com.ezh.Inventory.sales.delivery.dto;

import lombok.Data;

import java.util.Date;

@Data
public class RescheduleDeliveryDto {
    private Date newDate;
    private String reason;
}
