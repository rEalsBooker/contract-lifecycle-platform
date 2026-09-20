package com.contractguard.platform.ai.config;
import com.contractguard.platform.ai.extraction.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.rag")
public record RagProperties(boolean enabled, String embeddingModelName, int embeddingDimensions,
                            int chunkSize, int chunkOverlap, int topK, double minScore,
                            boolean fallbackEnabled, String qdrantUrl, String collectionName) {
    public RagProperties {
        if (embeddingDimensions <= 0) embeddingDimensions = 1024;
        if (chunkSize < 400) chunkSize = 1200;
        if (chunkOverlap < 0 || chunkOverlap >= chunkSize) chunkOverlap = 200;
        if (topK <= 0) topK = 4;
        if (minScore < 0 || minScore > 1) minScore = 0.55;
        if (collectionName == null || collectionName.isBlank()) collectionName = "contract_chunks";
    }
}


