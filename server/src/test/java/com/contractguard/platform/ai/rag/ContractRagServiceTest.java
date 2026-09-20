package com.contractguard.platform.ai.rag;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.extraction.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import com.contractguard.platform.contract.ContractRow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ContractRagServiceTest {
    private final RagProperties properties = new RagProperties(true, "embedding", 3, 400, 50,
            4, 0.55, true, "http://localhost:6333", "contract_chunks");

    @Test
    void buildsPersistentIndexWithScopedMetadata() {
        TextEmbeddingProvider embeddings = mock(TextEmbeddingProvider.class);
        ContractVectorStore store = mock(ContractVectorStore.class);
        ContractRagIndexMapper indexes = mock(ContractRagIndexMapper.class);
        when(embeddings.available()).thenReturn(true);
        when(embeddings.modelName()).thenReturn("embedding");
        when(embeddings.embedAll(anyList())).thenReturn(List.of(new float[]{1, 0, 0}));
        ContractRagService service = new ContractRagService(properties, embeddings, store,
                new ContractChunker(properties), indexes);
        ParseJobExecutionRow row = row();

        assertTrue(service.rebuildIndex(row, "[[PAGE:3]]\n验收后十五日内支付尾款。"));
        verify(store).replaceCurrentVersion(eq(1L), eq(2L), eq(3L), anyList(), anyList());
        verify(indexes).upsert(1L, 2L, 3L, "READY", 1, "embedding", null);
    }

    @Test
    void vectorSearchAlwaysCarriesTenantContractAndVersionFilter() {
        TextEmbeddingProvider embeddings = mock(TextEmbeddingProvider.class);
        ContractVectorStore store = mock(ContractVectorStore.class);
        ContractRagIndexMapper indexes = mock(ContractRagIndexMapper.class);
        when(embeddings.available()).thenReturn(true);
        when(embeddings.embed("尾款何时支付")).thenReturn(new float[]{1, 0, 0});
        when(indexes.ready(1L, 2L, 3L)).thenReturn(true);
        when(store.search(eq(1L), eq(2L), eq(3L), any(float[].class), eq(4), eq(0.55)))
                .thenReturn(List.of(new RagMatch(2L, 3L, 4L, 3, 0, "验收后十五日内支付尾款", 0.88)));
        ContractRagService service = new ContractRagService(properties, embeddings, store,
                new ContractChunker(properties), indexes);

        RagRetrievalResult result = service.retrieve(contract(), "尾款何时支付", null);

        assertEquals("VECTOR", result.mode());
        assertEquals(2L, result.matches().get(0).contractId());
        verify(store).search(eq(1L), eq(2L), eq(3L), any(float[].class), eq(4), eq(0.55));
    }

    @Test
    void fallsBackInsideSameContractWhenVectorStoreFails() {
        TextEmbeddingProvider embeddings = mock(TextEmbeddingProvider.class);
        ContractVectorStore store = mock(ContractVectorStore.class);
        ContractRagIndexMapper indexes = mock(ContractRagIndexMapper.class);
        when(embeddings.available()).thenReturn(true);
        when(embeddings.embed(anyString())).thenReturn(new float[]{1, 0, 0});
        when(indexes.ready(1L, 2L, 3L)).thenReturn(true);
        when(store.search(anyLong(), anyLong(), anyLong(), any(), anyInt(), anyDouble()))
                .thenThrow(new IllegalStateException("Qdrant unavailable"));
        ContractRagService service = new ContractRagService(properties, embeddings, store,
                new ContractChunker(properties), indexes);

        RagRetrievalResult result = service.retrieve(contract(), "尾款支付",
                "[[PAGE:8]]\n甲方应在验收后30日内完成尾款支付。" );

        assertEquals("NGRAM_FALLBACK", result.mode());
        assertEquals(2L, result.matches().get(0).contractId());
        assertNotNull(result.warning());
    }

    private ParseJobExecutionRow row() {
        ParseJobExecutionRow row = new ParseJobExecutionRow();
        row.setTenantId(1L); row.setContractId(2L); row.setContractVersionId(3L); row.setSourceFileId(4L);
        return row;
    }

    private ContractRow contract() {
        ContractRow row = new ContractRow();
        row.setTenantId(1L); row.setId(2L); row.setCurrentVersionId(3L); row.setSourceFileId(4L);
        return row;
    }
}


