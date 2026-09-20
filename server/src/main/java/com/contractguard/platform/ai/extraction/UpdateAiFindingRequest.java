package com.contractguard.platform.ai.extraction;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAiFindingRequest(@NotBlank @Size(max=32) String findingType,
                                     @NotBlank @Size(max=255) String title,
                                     @NotBlank @Size(max=10000) String content,
                                     Integer sourcePageNo, @Size(max=1000) String sourceExcerpt) { }


