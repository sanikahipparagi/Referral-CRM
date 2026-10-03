package com.referralcrm.service;

import com.referralcrm.integration.LLMProvider;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/** Optional AI boundary. Deterministic analyzers continue to work when there is no configured provider. */
@Service
public class LLMAnalysisService {
    private final ObjectProvider<LLMProvider> provider;
    public LLMAnalysisService(ObjectProvider<LLMProvider> provider) { this.provider=provider; }
    public Optional<String> analyze(String prompt,Map<String,String> context) {
        LLMProvider configured=provider.getIfAvailable();
        return configured==null?Optional.empty():Optional.ofNullable(configured.generate(prompt,context)).filter(text->!text.isBlank());
    }
}
