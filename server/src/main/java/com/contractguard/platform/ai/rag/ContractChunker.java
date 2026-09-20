package com.contractguard.platform.ai.rag;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.extraction.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ContractChunker {
    private static final Pattern PAGE = Pattern.compile("^\\[\\[PAGE:(\\d+)]]\\s*$");
    private final RagProperties properties;

    public ContractChunker(RagProperties properties) { this.properties = properties; }

    public List<ContractChunk> split(Long tenantId, Long contractId, Long versionId, Long fileId, String text) {
        List<Paragraph> paragraphs = paragraphs(text);
        List<ContractChunk> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        Integer firstPage = null;

        for (Paragraph paragraph : paragraphs) {
            if (current.length() > 0 && current.length() + paragraph.text().length() + 2 > properties.chunkSize()) {
                add(result, tenantId, contractId, versionId, fileId, firstPage, current.toString());
                String overlap = tail(current.toString(), properties.chunkOverlap());
                current.setLength(0);
                if (!overlap.isBlank()) current.append(overlap).append("\n");
                firstPage = paragraph.pageNo();
            }
            if (firstPage == null) firstPage = paragraph.pageNo();
            if (current.length() > 0) current.append("\n\n");
            current.append(paragraph.text());
        }
        if (!current.isEmpty()) add(result, tenantId, contractId, versionId, fileId, firstPage, current.toString());
        return result;
    }

    private List<Paragraph> paragraphs(String text) {
        List<Paragraph> result = new ArrayList<>();
        Integer page = null;
        StringBuilder paragraph = new StringBuilder();
        for (String line : text.replace("\r", "").split("\n")) {
            Matcher matcher = PAGE.matcher(line.trim());
            if (matcher.matches()) {
                flush(result, paragraph, page);
                page = Integer.valueOf(matcher.group(1));
            } else if (line.isBlank()) {
                flush(result, paragraph, page);
            } else {
                if (!paragraph.isEmpty()) paragraph.append('\n');
                paragraph.append(line.trim());
            }
        }
        flush(result, paragraph, page);
        return result;
    }

    private void flush(List<Paragraph> target, StringBuilder paragraph, Integer page) {
        if (!paragraph.isEmpty()) {
            String value = paragraph.toString().trim();
            for (int offset = 0; offset < value.length(); offset += properties.chunkSize()) {
                int end = Math.min(value.length(), offset + properties.chunkSize());
                target.add(new Paragraph(page, value.substring(offset, end)));
            }
            paragraph.setLength(0);
        }
    }

    private void add(List<ContractChunk> result, Long tenantId, Long contractId, Long versionId, Long fileId,
                     Integer pageNo, String value) {
        String text = value.trim();
        if (!text.isBlank()) result.add(new ContractChunk(tenantId, contractId, versionId, fileId,
                pageNo, result.size(), text));
    }

    private String tail(String value, int size) {
        if (size <= 0 || value.isBlank()) return "";
        int start = Math.max(0, value.length() - size);
        int boundary = value.indexOf('\n', start);
        return value.substring(boundary >= 0 ? boundary + 1 : start).trim();
    }

    private record Paragraph(Integer pageNo, String text) { }
}


