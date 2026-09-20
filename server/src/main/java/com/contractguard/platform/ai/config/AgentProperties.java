package com.contractguard.platform.ai.config;
import com.contractguard.platform.ai.extraction.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.agent")
public record AgentProperties(boolean enabled) { }


