package com.ezh.Inventory.utils.common;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommonFilter {
    private Long id;
    private String searchQuery;
    private String status;
    private Long warehouseId;
    private LocalDate fromDate;
    private LocalDate toDate;

    // Get 00:00:00 of the selected day
    public LocalDateTime getStartDateTime() {
        return fromDate != null ? fromDate.atStartOfDay() : null;
    }

    // Get 23:59:59.999999 of the selected day
    public LocalDateTime getEndDateTime() {
        return toDate != null ? toDate.atTime(LocalTime.MAX) : null;
    }
}
