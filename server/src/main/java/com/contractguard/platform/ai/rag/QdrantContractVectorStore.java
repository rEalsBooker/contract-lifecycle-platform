package com.contractguard.platform.ai.rag;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.extraction.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class QdrantContractVectorStore implements ContractVectorStore {
    private final RagProperties properties;
    private final ObjectMapper json;
    private final RestClient client;
    private volatile boolean collectionReady;

    public QdrantContractVectorStore(RagProperties properties, ObjectMapper json) {
        this.properties = properties;
        this.json = json;
        this.client = RestClient.builder().baseUrl(properties.qdrantUrl()).build();
    }

    @Override
    public void replaceCurrentVersion(Long tenantId, Long contractId, Long versionId,
                                      List<ContractChunk> chunks, List<float[]> vectors) {
        if (chunks.size() != vectors.size()) throw new IllegalArgumentException("Chunk 与向量数量不一致");
        ensureCollection();
        List<Map<String, Object>> points = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            ContractChunk chunk = chunks.get(i);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("tenantId", tenantId);
            payload.put("contractId", contractId);
            payload.put("contractVersionId", versionId);
            payload.put("fileId", chunk.fileId());
            if (chunk.pageNo() != null) payload.put("pageNo", chunk.pageNo());
            payload.put("chunkIndex", chunk.chunkIndex());
            payload.put("sourceText", chunk.sourceText());
            String seed = tenantId + ":" + contractId + ":" + versionId + ":" + chunk.chunkIndex();
            points.add(Map.of("id", UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString(),
                    "vector", vectors.get(i), "payload", payload));
        }
        if (!points.isEmpty()) {
            client.put().uri("/collections/{name}/points?wait=true", properties.collectionName())
                    .body(Map.of("points", points)).retrieve().toBodilessEntity();
        }
        Map<String, Object> filter = Map.of(
                "must", List.of(match("tenantId", tenantId), match("contractId", contractId)),
                "must_not", List.of(match("contractVersionId", versionId)));
        client.post().uri("/collections/{name}/points/delete?wait=true", properties.collectionName())
                .body(Map.of("filter", filter)).retrieve().toBodilessEntity();
    }

    @Override
    public List<RagMatch> search(Long tenantId, Long contractId, Long versionId, float[] queryVector,
                                 int topK, double minScore) {
        ensureCollection();
        Map<String, Object> filter = Map.of("must", List.of(
                match("tenantId", tenantId), match("contractId", contractId),
                match("contractVersionId", versionId)));
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("vector", queryVector);
        request.put("filter", filter);
        request.put("limit", topK);
        request.put("score_threshold", minScore);
        request.put("with_payload", true);
        String response = client.post().uri("/collections/{name}/points/search", properties.collectionName())
                .body(request).retrieve().body(String.class);
        try {
            List<RagMatch> matches = new ArrayList<>();
            for (JsonNode item : json.readTree(response).path("result")) {
                JsonNode payload = item.path("payload");
                matches.add(new RagMatch(payload.path("contractId").asLong(),
                        payload.path("contractVersionId").asLong(), payload.path("fileId").asLong(),
                        payload.has("pageNo") ? payload.path("pageNo").asInt() : null,
                        payload.path("chunkIndex").asInt(), payload.path("sourceText").asText(),
                        item.path("score").asDouble()));
            }
            return matches;
        } catch (Exception exception) {
            throw new IllegalStateException("Qdrant 返回格式无法解析", exception);
        }
    }

    private synchronized void ensureCollection() {
        if (collectionReady) return;
        try {
            client.get().uri("/collections/{name}", properties.collectionName()).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound notFound) {
            client.put().uri("/collections/{name}", properties.collectionName())
                    .body(Map.of("vectors", Map.of("size", properties.embeddingDimensions(), "distance", "Cosine")))
                    .retrieve().toBodilessEntity();
        }
        createPayloadIndex("tenantId", "integer");
        createPayloadIndex("contractId", "integer");
        createPayloadIndex("contractVersionId", "integer");
        collectionReady = true;
    }

    private void createPayloadIndex(String field, String type) {
        try {
            client.put().uri("/collections/{name}/index?wait=true", properties.collectionName())
                    .body(Map.of("field_name", field, "field_schema", type)).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException exception) {
            if (!HttpStatusCode.valueOf(exception.getStatusCode().value()).is2xxSuccessful()
                    && exception.getStatusCode().value() != 409) throw exception;
        }
    }

    private Map<String, Object> match(String key, Object value) {
        return Map.of("key", key, "match", Map.of("value", value));
    }
}


