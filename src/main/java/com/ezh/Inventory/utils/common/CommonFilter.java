package com.ezh.Inventory.utils.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class CommonFilter {
    private Long id;
    private String searchQuery;
    private List<String> statuses;
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
