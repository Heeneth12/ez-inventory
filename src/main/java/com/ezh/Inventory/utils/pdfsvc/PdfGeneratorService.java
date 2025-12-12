package com.ezh.Inventory.utils.pdfsvc;

import com.ezh.Inventory.sales.invoice.entity.Invoice;
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

    public byte[] generateInvoicePdf(Invoice invoice) throws DocumentException, IOException {
        // 1. Prepare Context (Data for Thymeleaf)
        Context context = new Context();
        context.setVariable("invoice", invoice);

        // 2. Process HTML Template
        // "invoice_pdf" matches the file name in resources/templates/invoice_pdf.html
        String htmlContent = templateEngine.process("invoice_pdf", context);

        // 3. Convert HTML to PDF using Flying Saucer
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        }
    }
}