package com.ezh.Inventory.utils;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@MappedSuperclass
public abstract class AbstractFinancialLine extends CommonSerializable {

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // The Percentage (e.g., 10 for 10%)
    @Column(name = "discount_rate")
    private BigDecimal discountRate = BigDecimal.ZERO;

    // The calculated value of the discount
    @Column(name = "discount_amount")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    // The Percentage (e.g., 18 for 18%)
    @Column(name = "tax_rate")
    private BigDecimal taxRate = BigDecimal.ZERO;

    // The calculated value of the tax
    @Column(name = "tax_amount")
    private BigDecimal taxAmount = BigDecimal.ZERO;

    // (Price * Qty) - DiscountAmount + TaxAmount
    @Column(name = "line_total", nullable = false)
    private BigDecimal lineTotal = BigDecimal.ZERO;
}
