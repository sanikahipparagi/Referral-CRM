package com.referralcrm.service;

import com.referralcrm.api.ApiException;
import com.referralcrm.domain.PromptTemplate;
import com.referralcrm.repository.PromptTemplateRepository;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromptTemplateService {
    public static final Set<String> CATEGORIES = Set.of("LINKEDIN_REFERRAL", "SHORT", "EMAIL", "FOLLOW_UP", "COLD_MESSAGE");
    private final PromptTemplateRepository repository;
    public PromptTemplateService(PromptTemplateRepository repository) { this.repository = repository; }

    public List<PromptTemplate> list(UUID userId) {
        Map<String, PromptTemplate> latest = new TreeMap<>();
        for (PromptTemplate template : repository.findByUserIdIsNullAndDeletedAtIsNullOrderByCategoryAscVersionDesc()) latest.putIfAbsent(template.getCategory(), template);
        for (PromptTemplate template : repository.findByUserIdAndDeletedAtIsNullOrderByCategoryAscVersionDesc(userId)) if (template.isActive()) latest.put(template.getCategory(), template);
        return new ArrayList<>(latest.values());
    }

    public PromptTemplate active(UUID userId, String category) {
        validateCategory(category);
        return repository.findFirstByUserIdAndCategoryAndActiveTrueAndDeletedAtIsNullOrderByVersionDesc(userId, category)
            .or(() -> repository.findFirstByUserIdIsNullAndCategoryAndDeletedAtIsNullOrderByVersionDesc(category))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Prompt template not found"));
    }

    public List<PromptTemplate> history(UUID userId,String category) {
        validateCategory(category);
        List<PromptTemplate> history=new ArrayList<>(repository.findByUserIdAndCategoryAndDeletedAtIsNullOrderByVersionDesc(userId,category));
        history.addAll(repository.findByUserIdIsNullAndCategoryAndDeletedAtIsNullOrderByVersionDesc(category));
        return history;
    }

    public String render(UUID userId, String category, Map<String, String> values) {
        String result = active(userId, category).getPromptText().replace("\\n", "\n");
        for (Map.Entry<String, String> value : values.entrySet()) result = result.replace("{" + value.getKey() + "}", value.getValue() == null ? "" : value.getValue());
        return result;
    }

    @Transactional
    public PromptTemplate saveVersion(UUID userId, String category, String promptText) {
        validateCategory(category);
        PromptTemplate current = repository.findFirstByUserIdAndCategoryOrderByVersionDesc(userId, category).orElse(null);
        if (current != null) current.setActive(false);
        PromptTemplate next = new PromptTemplate(); next.setUserId(userId); next.setCategory(category);
        int priorVersion=current==null?active(userId,category).getVersion():current.getVersion();
        next.setVersion(priorVersion+1); next.setPromptText(promptText.trim()); next.setActive(true);
        return repository.save(next);
    }

    private void validateCategory(String category) {
        if (!CATEGORIES.contains(category)) throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported prompt category");
    }
}
