package com.ezh.Inventory.sales.delivery.utils;

import com.ezh.Inventory.sales.delivery.dto.BulkDeliveryItemDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public final class DeliveryExportUtils {

    private static final String SHEET_NAME = "Bulk Pick List";
    private static final String[] HEADERS = {
            "Item ID", "Item Code", "SKU", "Category", "Brand", "Item Name", 
            "Batch Number", "Total Quantity", "MRP", "Selling Price", "Expiry Date"
    };

    private DeliveryExportUtils() {
    }

    public static ByteArrayInputStream toBulkItemsExcel(List<BulkDeliveryItemDto> rows) {
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
            for (BulkDeliveryItemDto rowData : rows) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(valueOf(rowData.getItemId()));
                row.createCell(1).setCellValue(valueOf(rowData.getItemCode()));
                row.createCell(2).setCellValue(valueOf(rowData.getSku()));
                row.createCell(3).setCellValue(valueOf(rowData.getCategory()));
                row.createCell(4).setCellValue(valueOf(rowData.getBrand()));
                row.createCell(5).setCellValue(valueOf(rowData.getItemName()));
                row.createCell(6).setCellValue(valueOf(rowData.getBatchNumber()));
                row.createCell(7).setCellValue(valueOf(rowData.getTotalQuantity()));
                row.createCell(8).setCellValue(valueOf(rowData.getMrp()));
                row.createCell(9).setCellValue(valueOf(rowData.getSellingPrice()));
                
                if (rowData.getExpiryDate() != null) {
                    java.util.Date expDate = new java.util.Date(rowData.getExpiryDate());
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                    row.createCell(10).setCellValue(sdf.format(expDate));
                } else {
                    row.createCell(10).setCellValue("");
                }
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate bulk delivery items excel", e);
        }
    }

    private static String valueOf(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
