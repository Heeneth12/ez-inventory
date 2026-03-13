package com.ezh.Inventory.sales.order.utils;

import com.ezh.Inventory.sales.order.dto.SalesOrderExcelRowDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public final class SalesOrderExportUtils {

    private static final String SHEET_NAME = "sales_orders";
    private static final String[] HEADERS = {
            "ID", "Order Number", "Order Date", "Status", "Source", "Customer ID", "Warehouse ID",
            "Item Gross Total", "Item Total Discount", "Item Total Tax", "Grand Total", "Remarks"
    };
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private SalesOrderExportUtils() {
    }

    public static ByteArrayInputStream toExcel(List<SalesOrderExcelRowDto> rows) {
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
            for (SalesOrderExcelRowDto rowData : rows) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(valueOf(rowData.getId()));
                row.createCell(1).setCellValue(valueOf(rowData.getOrderNumber()));
                row.createCell(2).setCellValue(rowData.getOrderDate() != null ? DATE_FORMAT.format(rowData.getOrderDate()) : "");
                row.createCell(3).setCellValue(valueOf(rowData.getStatus()));
                row.createCell(4).setCellValue(valueOf(rowData.getSource()));
                row.createCell(5).setCellValue(valueOf(rowData.getCustomerId()));
                row.createCell(6).setCellValue(valueOf(rowData.getWarehouseId()));
                row.createCell(7).setCellValue(valueOf(rowData.getItemGrossTotal()));
                row.createCell(8).setCellValue(valueOf(rowData.getItemTotalDiscount()));
                row.createCell(9).setCellValue(valueOf(rowData.getItemTotalTax()));
                row.createCell(10).setCellValue(valueOf(rowData.getGrandTotal()));
                row.createCell(11).setCellValue(valueOf(rowData.getRemarks()));
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate sales order excel", e);
        }
    }

    private static String valueOf(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
