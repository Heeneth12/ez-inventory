package com.ezh.Inventory.sales.delivery.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkDeliveryItemDto {
    private Long itemId;
    private String itemName;
    private String batchNumber;
    private Integer totalQuantity;
    
    // Additional Details
    private String itemCode;
    private String sku;
    private String category;
    private String brand;
    private java.math.BigDecimal mrp;
    private java.math.BigDecimal sellingPrice;
    private Long expiryDate;
}
