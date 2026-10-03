package com.referralcrm.service;

import com.referralcrm.domain.Company;
import com.referralcrm.domain.Contact;
import com.referralcrm.domain.RecommendationRule;
import com.referralcrm.repository.RecommendationRuleRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {
    public record Recommendation(int score, String reason) {}
    private final RecommendationRuleRepository rules;
    public RecommendationService(RecommendationRuleRepository rules) { this.rules = rules; }

    public Recommendation recommend(UUID userId, Contact contact, Company company, String targetRole) {
        String evidence = String.join(" ", safe(contact.getDesignation()), safe(contact.getNotes()), safe(targetRole), safe(company == null ? null : company.getName())).toLowerCase(Locale.ROOT);
        List<RecommendationRule> configured = rules.findByUserIdAndDeletedAtIsNullOrderByCategoryAscKeywordAsc(userId);
        List<RecommendationRule> active = configured.isEmpty() ? rules.findByUserIdIsNullAndEnabledTrueAndDeletedAtIsNull() : configured.stream().filter(RecommendationRule::isEnabled).toList();
        int score = 38 + (company == null ? 0 : Math.max(0, company.getPriority()) * 3);
        List<String> matched = new ArrayList<>();
        for (RecommendationRule rule : active) {
            String term = rule.getKeyword().toLowerCase(Locale.ROOT);
            if (!term.isBlank() && evidence.contains(term)) { score += rule.getWeight(); matched.add(rule.getKeyword()); }
        }
        if (contact.getStatus() == com.referralcrm.domain.ContactStatus.REPLIED || contact.getStatus() == com.referralcrm.domain.ContactStatus.REFERRED) score += 8;
        score = Math.min(100, score);
        String reason = matched.isEmpty() ? "Role and skills are not detailed yet; add context to improve this match." : "Matches " + String.join(", ", matched) + (company == null ? "." : "; linked to " + company.getName() + ".");
        if (contact.getDesignation() != null && (contact.getDesignation().toLowerCase(Locale.ROOT).contains("senior") || contact.getDesignation().toLowerCase(Locale.ROOT).contains("recruit") || contact.getDesignation().toLowerCase(Locale.ROOT).contains("manager") || contact.getDesignation().toLowerCase(Locale.ROOT).contains("staff"))) reason += " Likely referral contact based on the recorded role.";
        return new Recommendation(score, reason);
    }
    public List<RecommendationRule> listRules(UUID userId) {
        List<RecommendationRule> configured = rules.findByUserIdAndDeletedAtIsNullOrderByCategoryAscKeywordAsc(userId);
        return configured.isEmpty() ? rules.findByUserIdIsNullAndEnabledTrueAndDeletedAtIsNull() : configured;
    }
    @Transactional
    public List<RecommendationRule> replaceRules(UUID userId, List<RuleInput> inputs) {
        rules.findByUserIdAndDeletedAtIsNullOrderByCategoryAscKeywordAsc(userId).forEach(rule -> rule.setDeletedAt(OffsetDateTime.now()));
        List<RecommendationRule> saved = new ArrayList<>();
        for (RuleInput input : inputs) {
            RecommendationRule rule = new RecommendationRule(); rule.setUserId(userId); rule.setCategory(input.category().trim().toUpperCase(Locale.ROOT));
            rule.setKeyword(input.keyword().trim()); rule.setWeight(input.weight()); rule.setEnabled(input.enabled()); saved.add(rules.save(rule));
        }
        return saved;
    }
    public record RuleInput(String category, String keyword, short weight, boolean enabled) {}
    private String safe(String text) { return text == null ? "" : text; }
}
