package com.ezh.Inventory.utils.pdfsvc;

import com.ezh.Inventory.sales.invoice.entity.Invoice;
import com.ezh.Inventory.sales.payment.entity.Payment;
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
    public byte[] generatePaymentPdf(Payment payment, UserMiniDto customer) throws DocumentException, IOException {
        Context context = new Context();
        context.setVariable("payment", payment);
        context.setVariable("customer", customer);
        context.setVariable("allocations", payment.getAllocations());
        return renderPdf("payment_receipt_pdf", context);
    }

    // Private helper to keep code DRY
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