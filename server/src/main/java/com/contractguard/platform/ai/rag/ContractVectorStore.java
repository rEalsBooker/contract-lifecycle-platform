package com.contractguard.platform.ai.rag;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.extraction.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import java.util.List;

public interface ContractVectorStore {
    void replaceCurrentVersion(Long tenantId, Long contractId, Long versionId,
                               List<ContractChunk> chunks, List<float[]> vectors);
    List<RagMatch> search(Long tenantId, Long contractId, Long versionId, float[] queryVector,
                          int topK, double minScore);
}


