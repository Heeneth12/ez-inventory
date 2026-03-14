package com.ezh.Inventory.utils.common.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationDto {
    private Long id;
    private String appName;
    private String appKey;
    private String description;
    private Boolean isActive = true;
}
