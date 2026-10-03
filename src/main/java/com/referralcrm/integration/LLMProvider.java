package com.referralcrm.integration;

import java.util.Map;

/** Future port for a language model. No external provider is configured or called by this phase. */
public interface LLMProvider {
    String generate(String prompt, Map<String, String> context);
}
