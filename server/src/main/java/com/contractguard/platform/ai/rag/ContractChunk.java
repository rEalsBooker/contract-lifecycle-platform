package com.contractguard.platform.ai.rag;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.extraction.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

public record ContractChunk(Long tenantId, Long contractId, Long contractVersionId, Long fileId,
                            Integer pageNo, int chunkIndex, String sourceText) { }


