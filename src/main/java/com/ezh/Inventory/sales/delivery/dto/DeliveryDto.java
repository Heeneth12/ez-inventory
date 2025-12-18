package com.ezh.Inventory.sales.delivery.dto;

import com.ezh.Inventory.contacts.dto.ContactDto;
import com.ezh.Inventory.contacts.dto.ContactMiniDto;
import com.ezh.Inventory.employee.dto.EmployeeDto;
import com.ezh.Inventory.sales.delivery.entity.ShipmentStatus;
import com.ezh.Inventory.sales.delivery.entity.ShipmentType;
import com.ezh.Inventory.sales.invoice.dto.InvoiceDto;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryDto {
    private Long id;
    private Long tenantId;
    private String deliveryNumber;  // DEV-2025-001
    private InvoiceDto invoice;
    private ContactMiniDto contactMini;
    private ContactDto customer;
    private Long customerId;
    private String customerName;
    private ShipmentType type;   // PICKUP / COURIER / OWN_FLEET
    private ShipmentStatus status; // PENDING, SCHEDULED, SHIPPED, DELIVERED
    private EmployeeDto deliveryPerson;
    private Date scheduledDate;
    private Date shippedDate;
    private Date deliveredDate;
    private String deliveryAddress;
    private String contactPerson;
    private String contactPhone;
}
