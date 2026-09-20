package com.contractguard.platform.ai.rag;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.extraction.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import java.util.List;

public record RagRetrievalResult(String mode, List<RagMatch> matches, String warning) { }


