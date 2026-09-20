package com.contractguard.platform.ai.extraction;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import com.contractguard.platform.common.ApiException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Component
public class ContractTextExtractor {
    private static final int MAX_TEXT_LENGTH = 200_000;

    public String extract(InputStream stream, String contentType) {
        try {
            String text = contentType.contains("pdf") ? fromPdf(stream) : fromDocx(stream);
            String normalized = text.replace('\u0000', ' ').trim();
            if (normalized.isBlank()) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CONTRACT_TEXT_EMPTY",
                        "合同未提取到可读文字；MVP 暂不支持扫描件 OCR，请改用文本型 PDF/DOCX 或人工审阅");
            }
            return normalized.length() > MAX_TEXT_LENGTH ? normalized.substring(0, MAX_TEXT_LENGTH) : normalized;
        } catch (ApiException exception) { throw exception; }
        catch (Exception exception) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CONTRACT_TEXT_EXTRACT_FAILED", "合同文字提取失败，可切换人工审阅");
        }
    }

    private String fromPdf(InputStream stream) throws Exception {
        try (PDDocument document = Loader.loadPDF(readAll(stream))) {
            PDFTextStripper stripper = new PDFTextStripper();
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                text.append("[[PAGE:").append(page).append("]]\n")
                        .append(stripper.getText(document)).append('\n');
            }
            return text.toString();
        }
    }

    private String fromDocx(InputStream stream) throws Exception {
        try (XWPFDocument document = new XWPFDocument(stream)) {
            StringBuilder text = new StringBuilder();
            document.getParagraphs().forEach(paragraph -> text.append(paragraph.getText()).append('\n'));
            document.getTables().forEach(table -> table.getRows().forEach(row -> row.getTableCells()
                    .forEach(cell -> text.append(cell.getText()).append('\t'))));
            return text.toString();
        }
    }

    private byte[] readAll(InputStream stream) throws Exception {
        try (stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            stream.transferTo(output);
            return output.toByteArray();
        }
    }
}


