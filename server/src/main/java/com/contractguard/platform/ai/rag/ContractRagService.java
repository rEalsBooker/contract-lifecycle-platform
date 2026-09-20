package com.contractguard.platform.ai.rag;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.extraction.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import com.contractguard.platform.contract.ContractRow;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ContractRagService {
    private final RagProperties properties;
    private final TextEmbeddingProvider embeddings;
    private final ContractVectorStore vectorStore;
    private final ContractChunker chunker;
    private final ContractRagIndexMapper indexes;

    public ContractRagService(RagProperties properties, TextEmbeddingProvider embeddings,
                              ContractVectorStore vectorStore, ContractChunker chunker,
                              ContractRagIndexMapper indexes) {
        this.properties = properties; this.embeddings = embeddings; this.vectorStore = vectorStore;
        this.chunker = chunker; this.indexes = indexes;
    }

    public boolean rebuildIndex(ParseJobExecutionRow row, String extractedText) {
        if (!properties.enabled() || !embeddings.available()) return false;
        indexes.upsert(row.getTenantId(), row.getContractId(), row.getContractVersionId(),
                "INDEXING", 0, embeddings.modelName(), null);
        try {
            List<ContractChunk> chunks = chunker.split(row.getTenantId(), row.getContractId(),
                    row.getContractVersionId(), row.getSourceFileId(), extractedText);
            if (chunks.isEmpty()) throw new IllegalStateException("合同未提取到可索引文本");
            List<float[]> vectors = embeddings.embedAll(chunks.stream().map(ContractChunk::sourceText).toList());
            vectorStore.replaceCurrentVersion(row.getTenantId(), row.getContractId(), row.getContractVersionId(), chunks, vectors);
            indexes.upsert(row.getTenantId(), row.getContractId(), row.getContractVersionId(),
                    "READY", chunks.size(), embeddings.modelName(), null);
            return true;
        } catch (Exception exception) {
            indexes.upsert(row.getTenantId(), row.getContractId(), row.getContractVersionId(),
                    "FAILED", 0, embeddings.modelName(), limited(exception.getMessage()));
            return false;
        }
    }

    public RagRetrievalResult retrieve(ContractRow contract, String question, String fallbackText) {
        String warning = null;
        if (properties.enabled() && embeddings.available()
                && indexes.ready(contract.getTenantId(), contract.getId(), contract.getCurrentVersionId())) {
            try {
                List<RagMatch> matches = vectorStore.search(contract.getTenantId(), contract.getId(),
                        contract.getCurrentVersionId(), embeddings.embed(question), properties.topK(), properties.minScore());
                if (!matches.isEmpty()) return new RagRetrievalResult("VECTOR", matches, null);
                warning = "向量检索未找到达到阈值的合同原文";
            } catch (Exception exception) {
                warning = "向量检索暂不可用，已尝试合同内降级检索";
            }
        } else {
            warning = "当前合同尚未建立可用向量索引";
        }
        if (!properties.fallbackEnabled() || fallbackText == null || fallbackText.isBlank())
            return new RagRetrievalResult("NONE", List.of(), warning);
        return new RagRetrievalResult("NGRAM_FALLBACK", fallback(contract, question, fallbackText), warning);
    }

    private List<RagMatch> fallback(ContractRow contract, String question, String text) {
        List<ContractChunk> chunks = chunker.split(contract.getTenantId(), contract.getId(),
                contract.getCurrentVersionId(), contract.getSourceFileId(), text);
        Set<String> grams = grams(question);
        List<ScoredChunk> scored = new ArrayList<>();
        for (ContractChunk chunk : chunks) {
            int score = 0;
            for (String gram : grams) if (chunk.sourceText().contains(gram)) score++;
            if (score > 0) scored.add(new ScoredChunk(chunk, score));
        }
        scored.sort(Comparator.comparingInt(ScoredChunk::score).reversed());
        return scored.stream().limit(properties.topK()).map(value -> new RagMatch(contract.getId(),
                contract.getCurrentVersionId(), contract.getSourceFileId(), value.chunk().pageNo(),
                value.chunk().chunkIndex(), value.chunk().sourceText(), 0.0)).toList();
    }

    private Set<String> grams(String value) {
        String clean = value == null ? "" : value.replaceAll("\\s+", "");
        Set<String> set = new HashSet<>();
        for (int n = 2; n <= 4; n++) for (int i = 0; i + n <= clean.length(); i++) set.add(clean.substring(i, i + n));
        return set;
    }

    private String limited(String value) {
        String message = value == null ? "向量索引失败" : value;
        return message.substring(0, Math.min(message.length(), 1000));
    }

    private record ScoredChunk(ContractChunk chunk, int score) { }
}


