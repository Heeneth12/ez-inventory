package com.ezh.Inventory.purchase.po.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseOrderFilter {
    private Long id;
    private String searchQuery;
    private String status;
    private Long supplierId;
    private Long warehouseId;
    private LocalDate fromDate; // Use LocalDate for input
    private LocalDate toDate;   // Use LocalDate for input

    // Get 00:00:00 of the selected day
    public LocalDateTime getStartDateTime() {
        return fromDate != null ? fromDate.atStartOfDay() : null;
    }

    // Get 23:59:59.999999 of the selected day
    public LocalDateTime getEndDateTime() {
        return toDate != null ? toDate.atTime(LocalTime.MAX) : null;
    }
}