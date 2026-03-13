package com.ezh.Inventory.sales.invoice.utils;

import com.ezh.Inventory.sales.invoice.dto.InvoiceExcelRowDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public final class InvoiceExportUtils {

    private static final String SHEET_NAME = "invoices";
    private static final String[] HEADERS = {
            "ID", "Invoice Number", "Invoice Date", "Sales Order ID", "Sales Order Number", "Status", "Payment Status",
            "Customer ID", "Warehouse ID", "Item Gross Total", "Item Total Discount", "Item Total Tax",
            "Grand Total", "Amount Paid", "Balance", "Remarks"
    };
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private InvoiceExportUtils() {
    }

    public static ByteArrayInputStream toExcel(List<InvoiceExcelRowDto> rows) {
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
            for (InvoiceExcelRowDto rowData : rows) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(valueOf(rowData.getId()));
                row.createCell(1).setCellValue(valueOf(rowData.getInvoiceNumber()));
                row.createCell(2).setCellValue(rowData.getInvoiceDate() != null ? DATE_FORMAT.format(rowData.getInvoiceDate()) : "");
                row.createCell(3).setCellValue(valueOf(rowData.getSalesOrderId()));
                row.createCell(4).setCellValue(valueOf(rowData.getSalesOrderNumber()));
                row.createCell(5).setCellValue(valueOf(rowData.getStatus()));
                row.createCell(6).setCellValue(valueOf(rowData.getPaymentStatus()));
                row.createCell(7).setCellValue(valueOf(rowData.getCustomerId()));
                row.createCell(8).setCellValue(valueOf(rowData.getWarehouseId()));
                row.createCell(9).setCellValue(valueOf(rowData.getItemGrossTotal()));
                row.createCell(10).setCellValue(valueOf(rowData.getItemTotalDiscount()));
                row.createCell(11).setCellValue(valueOf(rowData.getItemTotalTax()));
                row.createCell(12).setCellValue(valueOf(rowData.getGrandTotal()));
                row.createCell(13).setCellValue(valueOf(rowData.getAmountPaid()));
                row.createCell(14).setCellValue(valueOf(rowData.getBalance()));
                row.createCell(15).setCellValue(valueOf(rowData.getRemarks()));
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate invoice excel", e);
        }
    }

    private static String valueOf(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
