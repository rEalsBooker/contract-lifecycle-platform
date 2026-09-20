package com.contractguard.platform.ai.extraction;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiStatusController {
    private final ContractExtractionModel model;

    public AiStatusController(ContractExtractionModel model) { this.model = model; }

    @GetMapping("/status")
    public AiStatusResponse status() {
        return new AiStatusResponse(model.available(), model.providerName(), model.modelName(),
                model.available() ? "AI_ASSISTED_REVIEW" : "MANUAL_REVIEW",
                "AI 仅提供辅助提取和风险提示，所有结果必须人工确认，不能替代法律意见");
    }
}


