package com.contractguard.platform.ai.config;
import com.contractguard.platform.ai.extraction.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(String provider, String apiKey, String modelName, String baseUrl, int timeoutSeconds) {
    public boolean compatibleModelConfigured() {
        return ("openai".equalsIgnoreCase(provider) || "qwen".equalsIgnoreCase(provider))
                && apiKey != null && !apiKey.isBlank()
                && modelName != null && !modelName.isBlank();
    }
}


