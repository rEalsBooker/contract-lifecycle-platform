package com.contractguard.platform.ai.rag;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.extraction.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContractChunkerTest {
    @Test
    void keepsTenantContractVersionAndPageMetadata() {
        RagProperties properties = new RagProperties(true, "embedding", 1024, 400, 50,
                4, 0.55, true, "http://localhost:6333", "contract_chunks");
        ContractChunker chunker = new ContractChunker(properties);
        String text = "[[PAGE:1]]\n第一条 交付要求。\n\n第二条 验收要求。\n[[PAGE:2]]\n第三条 付款要求。";

        List<ContractChunk> chunks = chunker.split(10L, 20L, 30L, 40L, text);

        assertFalse(chunks.isEmpty());
        assertEquals(10L, chunks.get(0).tenantId());
        assertEquals(20L, chunks.get(0).contractId());
        assertEquals(30L, chunks.get(0).contractVersionId());
        assertEquals(40L, chunks.get(0).fileId());
        assertEquals(1, chunks.get(0).pageNo());
        assertTrue(chunks.stream().allMatch(chunk -> !chunk.sourceText().contains("[[PAGE:")));
    }
}


