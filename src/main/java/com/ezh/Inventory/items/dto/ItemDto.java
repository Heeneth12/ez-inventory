package com.ezh.Inventory.items.dto;

import com.ezh.Inventory.items.entity.ItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto {
    private Long id;
    private String name;
    private String itemCode;
    private String sku; // Stock Keeping Unit
    private String barcode; // UPC / EAN / QR
    private ItemType itemType;
    private String imageUrl;
    private String category;
    private String unitOfMeasure;
    private String brand;
    private String manufacturer;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private BigDecimal mrp;
    private BigDecimal taxPercentage;
    private BigDecimal discountPercentage;
    private String hsnSacCode;
    private String description;
    private Boolean isActive;
}
