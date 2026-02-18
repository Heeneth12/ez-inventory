package com.ezh.Inventory.stock.dto;

import com.ezh.Inventory.stock.entity.MovementType;
import com.ezh.Inventory.utils.common.CommonFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class StockLedgerFilter extends CommonFilter {
     private List<MovementType> transactionTypes;
     private List<String> referenceTypes;
}
