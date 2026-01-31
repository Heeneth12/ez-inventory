package com.ezh.Inventory.purchase.po.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

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
    private Date fromDate;
    private Date toDate;
}