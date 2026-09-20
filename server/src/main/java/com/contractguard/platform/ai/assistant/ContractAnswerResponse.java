package com.contractguard.platform.ai.assistant;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.extraction.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import java.util.List;
public record ContractAnswerResponse(String answer, List<String> sources, List<RagSourceResponse> ragSources,
                                     List<String> toolsUsed, String retrievalMode,
                                     String modelName, String boundary, String warning) { }


