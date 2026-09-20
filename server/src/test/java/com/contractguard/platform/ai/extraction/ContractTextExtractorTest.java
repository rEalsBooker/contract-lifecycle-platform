package com.contractguard.platform.ai.extraction;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import org.junit.jupiter.api.Test;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ContractTextExtractorTest {
    @Test
    void extractsTextAndStablePageMarkerFromTextPdf() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(72, 720);
                content.showText("Payment due in 30 days.");
                content.endText();
            }
            document.save(bytes);
        }
        String text = new ContractTextExtractor().extract(new ByteArrayInputStream(bytes.toByteArray()), "application/pdf");
        assertFalse(text.isBlank());
        org.junit.jupiter.api.Assertions.assertTrue(text.contains("[[PAGE:1]]"));
        org.junit.jupiter.api.Assertions.assertTrue(text.contains("Payment due in 30 days."));
    }
}


