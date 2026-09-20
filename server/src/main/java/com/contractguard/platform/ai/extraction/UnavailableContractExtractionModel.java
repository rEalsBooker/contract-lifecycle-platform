package com.contractguard.platform.ai.extraction;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

public class UnavailableContractExtractionModel implements ContractExtractionModel {
    @Override public boolean available() { return false; }
    @Override public String providerName() { return "none"; }
    @Override public String modelName() { return "未配置"; }
    @Override public ContractExtractionResult extract(String contractText) {
        throw new IllegalStateException("AI 模型未配置");
    }
    @Override public String answer(String question, String retrievedContractText, String toolContext) {
        throw new IllegalStateException("AI 模型未配置");
    }
}


