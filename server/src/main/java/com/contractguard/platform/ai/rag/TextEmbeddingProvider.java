package com.contractguard.platform.ai.rag;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.extraction.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import java.util.List;

public interface TextEmbeddingProvider {
    boolean available();
    String modelName();
    int dimensions();
    List<float[]> embedAll(List<String> texts);
    float[] embed(String text);
}


