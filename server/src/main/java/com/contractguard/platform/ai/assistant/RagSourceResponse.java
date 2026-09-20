package com.contractguard.platform.ai.assistant;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.extraction.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;

public record RagSourceResponse(Long contractId, Long contractVersionId, Long fileId, Integer pageNo,
                                int chunkIndex, String sourceText, double score) {
    static RagSourceResponse from(RagMatch match) {
        return new RagSourceResponse(match.contractId(), match.contractVersionId(), match.fileId(), match.pageNo(),
                match.chunkIndex(), match.sourceText(), match.score());
    }
}


