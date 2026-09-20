package com.contractguard.platform.ai.config;
import com.contractguard.platform.ai.extraction.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfiguration {
    @Bean
    @ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
    ContractExtractionModel openAiContractExtractionModel(AiProperties properties, ObjectMapper objectMapper) {
        return new OpenAiContractExtractionModel(properties, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "app.ai.provider", havingValue = "qwen")
    ContractExtractionModel qwenContractExtractionModel(AiProperties properties, ObjectMapper objectMapper) {
        return new OpenAiContractExtractionModel(properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(ContractExtractionModel.class)
    ContractExtractionModel unavailableContractExtractionModel() {
        return new UnavailableContractExtractionModel();
    }

    @Bean
    TextEmbeddingProvider textEmbeddingProvider(AiProperties aiProperties, RagProperties ragProperties) {
        return new LangChain4jTextEmbeddingProvider(aiProperties, ragProperties);
    }
}


