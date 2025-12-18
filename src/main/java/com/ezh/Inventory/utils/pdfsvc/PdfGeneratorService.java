package com.ezh.Inventory.utils.pdfsvc;

import com.ezh.Inventory.sales.invoice.entity.Invoice;
import com.ezh.Inventory.sales.payment.dto.PaymentDto;
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
    public byte[] generateInvoicePdf(Invoice invoice) throws DocumentException, IOException {
        Context context = new Context();
        context.setVariable("invoice", invoice);
        return renderPdf("invoice_pdf", context);
    }

    /**
     * Generates a Payment Confirmation / Receipt PDF
     */
    public byte[] generatePaymentPdf(PaymentDto payment) throws DocumentException, IOException {
        Context context = new Context();
        context.setVariable("payment", payment);
        // We pass the allocations explicitly for easier access in Thymeleaf if needed
        context.setVariable("allocations", payment.getAllocatedAmount());

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