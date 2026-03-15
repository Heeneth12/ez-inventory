package com.ezh.Inventory.utils.pdfsvc;

import com.ezh.Inventory.sales.invoice.entity.Invoice;
import com.ezh.Inventory.sales.payment.entity.Payment;
import com.ezh.Inventory.sales.invoice.entity.InvoiceItem;
import com.ezh.Inventory.sales.returns.entity.SalesReturn;
import com.ezh.Inventory.sales.returns.entity.SalesReturnItem;
import com.ezh.Inventory.utils.common.dto.TenantDto;
import com.ezh.Inventory.utils.common.dto.UserMiniDto;
import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfGeneratorService {

    private final TemplateEngine templateEngine;

    // Existing Invoice Method
    public byte[] generateInvoicePdf(Invoice invoice, UserMiniDto customer, TenantDto tenant) throws DocumentException, IOException {
        Context context = new Context();
        context.setVariable("tenant", tenant);
        context.setVariable("invoice", invoice);
        context.setVariable("customer", customer);
        return renderPdf("invoice_pdf", context);
    }

    /**
     * Generates a Payment Confirmation / Receipt PDF
     */
    public byte[] generatePaymentPdf(Payment payment, UserMiniDto customer, TenantDto tenant) throws DocumentException, IOException {
        Context context = new Context();
        context.setVariable("tenant", tenant);
        context.setVariable("payment", payment);
        context.setVariable("customer", customer);
        context.setVariable("allocations", payment.getAllocations());
        return renderPdf("payment_receipt_pdf", context);
    }

    // Private helper to keep code DRY
    public byte[] generateSalesReturnPdf(SalesReturn salesReturn, UserMiniDto customer, TenantDto tenant) throws DocumentException, IOException {
        Context context = new Context();
        context.setVariable("tenant", tenant);
        context.setVariable("salesReturn", salesReturn);
        context.setVariable("invoice", salesReturn.getInvoice());
        context.setVariable("customer", customer);

        List<Map<String, Object>> displayItems = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal totalTaxAmount = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (SalesReturnItem rItem : salesReturn.getItems()) {
            Map<String, Object> map = new HashMap<>();
            map.put("quantity", rItem.getQuantity());
            map.put("unitPrice", rItem.getUnitPrice());
            map.put("reason", rItem.getReason() != null ? rItem.getReason() : "Return");
            map.put("batchNumber", rItem.getBatchNumber());

            BigDecimal lineTotal = rItem.getUnitPrice().multiply(new BigDecimal(rItem.getQuantity()));
            map.put("lineTotal", lineTotal);

            totalQuantity += rItem.getQuantity();
            grandTotal = grandTotal.add(lineTotal);

            // Match with original invoice item to extract metadata (itemName, sku, tax info)
            InvoiceItem match = salesReturn.getInvoice().getItems().stream()
                    .filter(i -> i.getItemId().equals(rItem.getItemId()))
                    .findFirst()
                    .orElse(null);

            if (match != null) {
                map.put("itemName", match.getItemName());
                map.put("sku", match.getSku());
                map.put("taxRate", match.getTaxRate());
                
                if (match.getTaxRate() != null && match.getTaxRate().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal taxAmount = lineTotal.multiply(match.getTaxRate()).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                    map.put("taxAmount", taxAmount);
                    totalTaxAmount = totalTaxAmount.add(taxAmount);
                    map.put("taxableValue", lineTotal.subtract(taxAmount)); // Since unit price generally includes tax or we subtract depending on structure. Wait, lineTotal is gross. So subtract tax to get taxable.
                } else {
                    map.put("taxRate", BigDecimal.ZERO);
                    map.put("taxAmount", BigDecimal.ZERO);
                    map.put("taxableValue", lineTotal);
                }
            } else {
                map.put("itemName", "Item ID: " + rItem.getItemId());
                map.put("sku", "");
                map.put("taxRate", BigDecimal.ZERO);
                map.put("taxAmount", BigDecimal.ZERO);
                map.put("taxableValue", lineTotal);
            }
            displayItems.add(map);
        }

        context.setVariable("displayItems", displayItems);
        context.setVariable("totalQuantity", totalQuantity);
        context.setVariable("itemTotalTax", totalTaxAmount);
        context.setVariable("grandTotal", salesReturn.getTotalAmount());

        return renderPdf("sales_return_pdf", context);
    }

    private byte[] renderPdf(String templateName, Context context) throws DocumentException, IOException {
        String htmlContent = templateEngine.process(templateName, context);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        }
    }
}