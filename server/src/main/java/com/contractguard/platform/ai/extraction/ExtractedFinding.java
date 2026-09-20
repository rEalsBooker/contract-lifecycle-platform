package com.contractguard.platform.ai.extraction;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import java.math.BigDecimal;

public record ExtractedFinding(String findingType, String title, String content, Integer sourcePageNo,
                               String sourceExcerpt, BigDecimal confidence) { }


