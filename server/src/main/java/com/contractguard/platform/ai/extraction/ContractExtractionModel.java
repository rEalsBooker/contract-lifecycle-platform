package com.contractguard.platform.ai.extraction;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import dev.langchain4j.model.chat.ChatModel;

public interface ContractExtractionModel {
    boolean available();
    String providerName();
    String modelName();
    ContractExtractionResult extract(String contractText);
    String answer(String question, String retrievedContractText, String toolContext);
    default ChatModel chatModel() { return null; }
}


