package com.referralcrm.service;

import com.referralcrm.domain.*;
import com.referralcrm.repository.GeneratedMessageRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageGenerationService {
    public record DraftSpec(String variant, String category, String channel) {}
    private static final List<DraftSpec> VARIANTS = List.of(
        new DraftSpec("LINKEDIN_REFERRAL", "LINKEDIN_REFERRAL", "LINKEDIN"),
        new DraftSpec("SHORT", "SHORT", "LINKEDIN"),
        new DraftSpec("EMAIL", "EMAIL", "EMAIL"),
        new DraftSpec("FOLLOW_UP", "FOLLOW_UP", "LINKEDIN"));
    private final GeneratedMessageRepository messages;
    private final PromptTemplateService prompts;
    public MessageGenerationService(GeneratedMessageRepository messages, PromptTemplateService prompts) { this.messages = messages; this.prompts = prompts; }

    @Transactional
    public List<GeneratedMessage> generate(UUID userId, AppUser user, Contact contact, Company company, Resume resume, String role, RecommendationService.Recommendation recommendation, String resumeReason, boolean followUpOnly) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("contactName", contact.getName()); values.put("designation", text(contact.getDesignation(), "a professional"));
        values.put("companyName", text(company == null ? null : company.getName(), "your organization")); values.put("role", text(role, "roles aligned with my background"));
        values.put("resumeLabel", resume == null ? "resume" : resume.getLabel()); values.put("userName", user.getFullName());
        values.put("profileSummary", profileSummary(user.getProfileText()));
        return VARIANTS.stream().filter(spec -> !followUpOnly || spec.variant().equals("FOLLOW_UP")).map(spec -> {
            String body = prompts.render(userId, spec.category(), values);
            int version = messages.findFirstByContactIdAndVariantAndDeletedAtIsNullOrderByVersionDesc(contact.getId(), spec.variant()).map(m -> m.getVersion() + 1).orElse(1);
            GeneratedMessage draft = new GeneratedMessage(); draft.setUserId(userId); draft.setContactId(contact.getId());
            draft.setCompanyId(company == null ? null : company.getId()); draft.setResumeId(resume == null ? null : resume.getId());
            draft.setRole(role); draft.setVariant(spec.variant()); draft.setVersion(version); draft.setChannel(spec.channel()); draft.setMessageText(body);
            draft.setRecommendationScore((short)recommendation.score()); draft.setRecommendationReason(recommendation.reason());
            draft.setResumeReason(resumeReason); draft.setStatus("DRAFT"); return messages.save(draft);
        }).toList();
    }

    private String text(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
    private String profileSummary(String value) {
        if (value == null || value.isBlank()) return "my professional background";
        String normalized = value.strip().replaceAll("\\s+", " ");
        return normalized.length() > 180 ? normalized.substring(0, 177) + "..." : normalized;
    }
}
