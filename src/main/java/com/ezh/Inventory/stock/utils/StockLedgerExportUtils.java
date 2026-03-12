package com.ezh.Inventory.stock.utils;

import com.ezh.Inventory.stock.dto.StockLedgerDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;

public final class StockLedgerExportUtils {

    private static final String SHEET_NAME = "stock_ledger";
    private static final String[] HEADERS = {
            "Ledger ID", "Item ID", "Item Name", "Warehouse ID", "Transaction Type", "Quantity",
            "Reference Type", "Reference ID", "Before Qty", "After Qty", "Created At"
    };
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private StockLedgerExportUtils() {
    }

    public static ByteArrayInputStream toExcel(List<StockLedgerDto> stockLedgers) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (StockLedgerDto ledger : stockLedgers) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(valueOf(ledger.getId()));
                row.createCell(1).setCellValue(valueOf(ledger.getItemId()));
                row.createCell(2).setCellValue(valueOf(ledger.getItemName()));
                row.createCell(3).setCellValue(valueOf(ledger.getWarehouseId()));
                row.createCell(4).setCellValue(valueOf(ledger.getTransactionType()));
                row.createCell(5).setCellValue(valueOf(ledger.getQuantity()));
                row.createCell(6).setCellValue(valueOf(ledger.getReferenceType()));
                row.createCell(7).setCellValue(valueOf(ledger.getReferenceId()));
                row.createCell(8).setCellValue(valueOf(ledger.getBeforeQty()));
                row.createCell(9).setCellValue(valueOf(ledger.getAfterQty()));
                row.createCell(10).setCellValue(ledger.getCreatedAt() != null ? DATE_FORMAT.format(ledger.getCreatedAt()) : "");
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate stock ledger excel", e);
        }
    }

    public static ByteArrayInputStream toCsv(List<StockLedgerDto> stockLedgers) {
        StringBuilder builder = new StringBuilder(String.join(",", HEADERS)).append("\n");

        for (StockLedgerDto ledger : stockLedgers) {
            builder.append(csvValue(ledger.getId())).append(",")
                    .append(csvValue(ledger.getItemId())).append(",")
                    .append(csvValue(ledger.getItemName())).append(",")
                    .append(csvValue(ledger.getWarehouseId())).append(",")
                    .append(csvValue(ledger.getTransactionType())).append(",")
                    .append(csvValue(ledger.getQuantity())).append(",")
                    .append(csvValue(ledger.getReferenceType())).append(",")
                    .append(csvValue(ledger.getReferenceId())).append(",")
                    .append(csvValue(ledger.getBeforeQty())).append(",")
                    .append(csvValue(ledger.getAfterQty())).append(",")
                    .append(csvValue(ledger.getCreatedAt() != null ? DATE_FORMAT.format(ledger.getCreatedAt()) : ""))
                    .append("\n");
        }

        return new ByteArrayInputStream(builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String valueOf(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String csvValue(Object value) {
        String valueStr = valueOf(value).replace("\"", "\"\"");
        return "\"" + valueStr + "\"";
    }
}
