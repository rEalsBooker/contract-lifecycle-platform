package com.contractguard.platform.ai.rag;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.extraction.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class LangChain4jTextEmbeddingProvider implements TextEmbeddingProvider {
    private final String modelName;
    private final int dimensions;
    private final EmbeddingModel model;

    public LangChain4jTextEmbeddingProvider(AiProperties ai, RagProperties rag) {
        this.modelName = rag.embeddingModelName();
        this.dimensions = rag.embeddingDimensions();
        this.model = rag.enabled() && ai.compatibleModelConfigured()
                && modelName != null && !modelName.isBlank()
                ? OpenAiEmbeddingModel.builder()
                    .apiKey(ai.apiKey())
                    .baseUrl(ai.baseUrl())
                    .modelName(modelName)
                    .dimensions(dimensions)
                    .timeout(Duration.ofSeconds(ai.timeoutSeconds()))
                    .maxRetries(2)
                    .build()
                : null;
    }

    @Override public boolean available() { return model != null; }
    @Override public String modelName() { return modelName == null ? "未配置" : modelName; }
    @Override public int dimensions() { return dimensions; }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        if (model == null) throw new IllegalStateException("Embedding 模型未配置");
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (int start = 0; start < texts.size(); start += 10) {
            int end = Math.min(start + 10, texts.size());
            List<TextSegment> segments = texts.subList(start, end).stream().map(TextSegment::from).toList();
            vectors.addAll(model.embedAll(segments).content().stream().map(value -> value.vector()).toList());
        }
        return vectors;
    }

    @Override
    public float[] embed(String text) {
        if (model == null) throw new IllegalStateException("Embedding 模型未配置");
        return model.embed(text).content().vector();
    }
}


