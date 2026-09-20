package com.contractguard.platform.ai.assistant;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.extraction.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.Size;
public record AskContractQuestionRequest(@NotBlank @Size(max=1000) String question) { }


