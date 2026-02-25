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
public abstract class AbstractFinancialHeader extends CommonSerializable {

    @Column(name = "item_gross_total") // Sum of (UnitPrice * Qty) from lines
    private BigDecimal itemGrossTotal = BigDecimal.ZERO;

    @Column(name = "item_total_discount") // Sum of line-level discount_amounts
    private BigDecimal itemTotalDiscount = BigDecimal.ZERO;

    @Column(name = "item_total_tax") // Sum of line-level tax_amounts
    private BigDecimal itemTotalTax = BigDecimal.ZERO;

    @Column(name = "flat_discount_rate") // e.g., 2% flat on total bill
    private BigDecimal flatDiscountRate = BigDecimal.ZERO;

    @Column(name = "flat_discount_amount") // The calculated dollar value of that 2%
    private BigDecimal flatDiscountAmount = BigDecimal.ZERO;

    @Column(name = "flat_tax_rate")
    private BigDecimal flatTaxRate = BigDecimal.ZERO;

    @Column(name = "flat_tax_amount")
    private BigDecimal flatTaxAmount = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false)
    private BigDecimal grandTotal = BigDecimal.ZERO;
}