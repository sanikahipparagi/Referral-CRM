package com.referralcrm.api;

import com.referralcrm.domain.*;
import com.referralcrm.repository.*;
import com.referralcrm.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {
    private final ContactRepository contacts; private final CompanyRepository companies; private final ResumeRepository resumes;
    private final AppUserRepository users; private final OutreachRepository outreach; private final GeneratedMessageRepository messages;
    private final OpportunityService opportunities; private final RecommendationService recommendations; private final ResumeRecommendationService resumeRecommendations;
    private final MessageGenerationService generation; private final PromptTemplateService prompts; private final AnalyticsService analytics;
    private final MessageWorkflowService workflow;
    private final int followUpDays;

    public AssistantController(ContactRepository contacts, CompanyRepository companies, ResumeRepository resumes, AppUserRepository users,
                               OutreachRepository outreach, GeneratedMessageRepository messages, OpportunityService opportunities,
                               RecommendationService recommendations, ResumeRecommendationService resumeRecommendations,
                               MessageGenerationService generation, PromptTemplateService prompts, AnalyticsService analytics, MessageWorkflowService workflow,
                               @Value("${app.follow-up-days:7}") int followUpDays) {
        this.contacts=contacts; this.companies=companies; this.resumes=resumes; this.users=users; this.outreach=outreach; this.messages=messages;
        this.opportunities=opportunities; this.recommendations=recommendations; this.resumeRecommendations=resumeRecommendations;
        this.generation=generation; this.prompts=prompts; this.analytics=analytics;
        this.workflow=workflow;
        this.followUpDays=Math.max(1,Math.min(followUpDays,365));
    }

    private UUID uid(Authentication auth) { return (UUID)auth.getPrincipal(); }
    private Contact contact(UUID id, UUID user) { return contacts.findById(id).filter(c -> c.getDeletedAt()==null && c.getUserId().equals(user)).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Contact not found")); }
    private Company company(UUID id, UUID user) { if(id==null) return null; return companies.findById(id).filter(c -> c.getDeletedAt()==null && c.getUserId().equals(user)).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Company not found")); }
    private Resume resume(UUID id, UUID user) { if(id==null) return null; return resumes.findById(id).filter(r -> r.getDeletedAt()==null && r.getUserId().equals(user)).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Resume not found")); }
    private AppUser user(UUID id) { return users.findById(id).filter(u -> u.getDeletedAt()==null).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,"Session is no longer valid")); }
    private GeneratedMessage message(UUID id, UUID user) { return messages.findByIdAndUserIdAndDeletedAtIsNull(id,user).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Message draft not found")); }

    public record QueueFilters(UUID companyId, String role, String status, String location, Integer priority) {}
    @GetMapping("/opportunities") public OpportunityService.Queue opportunities(Authentication auth,
        @RequestParam(required=false) UUID companyId,@RequestParam(required=false) String role,@RequestParam(required=false) String status,
        @RequestParam(required=false) String location,@RequestParam(required=false) @Min(1) @Max(5) Integer priority) {
        return opportunities.find(uid(auth),companyId,role,status,location,priority);
    }

    public record GenerateRequest(@NotNull UUID contactId, UUID resumeId, @Size(max=200) String role) {}
    public record MessageView(UUID id, UUID contactId, String contactName, String linkedinUrl, UUID companyId, String companyName,
                              UUID resumeId, String resumeLabel, String resumeReason, String role, String variant, int version,
                              String channel, String messageText, int recommendationScore, String recommendationReason,
                              String status, OffsetDateTime createdAt) {}
    @PostMapping("/messages/generate") @ResponseStatus(HttpStatus.CREATED)
    public List<MessageView> generate(Authentication auth,@Valid @RequestBody GenerateRequest request) {
        UUID userId=uid(auth); Contact c=contact(request.contactId(),userId); Company co=company(c.getCompanyId(),userId); AppUser current=user(userId);
        String role=request.role()==null||request.role().isBlank()?c.getDesignation():request.role().trim();
        Resume selected=resume(request.resumeId(),userId);
        ResumeRecommendationService.Result rec=selected==null?resumeRecommendations.recommend(resumes.findByUserIdAndDeletedAtIsNull(userId),role):new ResumeRecommendationService.Result(selected,"Selected by you.");
        RecommendationService.Recommendation score=recommendations.recommend(userId,c,co,role);
        return generation.generate(userId,current,c,co,rec.resume(),role,score,rec.reason(),false).stream().map(m->view(m,c,co,rec.resume())).toList();
    }

    @PostMapping("/follow-up/generate") @ResponseStatus(HttpStatus.CREATED)
    public MessageView followUp(Authentication auth,@Valid @RequestBody GenerateRequest request) {
        UUID userId=uid(auth); Contact c=contact(request.contactId(),userId);
        if(c.getRepliedAt()!=null || c.getStatus()==ContactStatus.REPLIED || c.getStatus()==ContactStatus.REFERRED || c.getStatus()==ContactStatus.INTERVIEW)
            throw new ApiException(HttpStatus.CONFLICT,"Follow-ups are disabled after a reply");
        OffsetDateTime cutoff=OffsetDateTime.now().minusDays(followUpDays);
        boolean due=outreach.findNeedsFollowUp(userId,cutoff).stream().anyMatch(row->row.getId().equals(c.getId()));
        if(!due) throw new ApiException(HttpStatus.CONFLICT,"This contact is not due for a follow-up yet");
        Company co=company(c.getCompanyId(),userId); Resume selected=resume(request.resumeId(),userId);
        var resumeRec=selected==null?resumeRecommendations.recommend(resumes.findByUserIdAndDeletedAtIsNull(userId),request.role()):new ResumeRecommendationService.Result(selected,"Selected by you.");
        var score=recommendations.recommend(userId,c,co,request.role());
        return generation.generate(userId,user(userId),c,co,resumeRec.resume(),request.role(),score,resumeRec.reason(),true).stream().map(m->view(m,c,co,resumeRec.resume())).findFirst().orElseThrow();
    }

    @GetMapping("/messages") public List<MessageView> listMessages(Authentication auth,@RequestParam(required=false) String status) {
        UUID userId=uid(auth);
        Map<String,GeneratedMessage> latest=new LinkedHashMap<>();
        messages.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId).stream().filter(m->!"SUPERSEDED".equals(m.getStatus()))
            .sorted(Comparator.comparingInt(GeneratedMessage::getVersion).reversed()).forEach(m->latest.putIfAbsent(m.getContactId()+":"+m.getVariant(),m));
        return latest.values().stream().filter(m->status==null||m.getStatus().equalsIgnoreCase(status)).map(m->{ Contact c=contact(m.getContactId(),userId); return view(m,c,company(m.getCompanyId(),userId),resume(m.getResumeId(),userId)); }).toList();
    }

    public record EditMessageRequest(@NotBlank @Size(max=10000) String messageText) {}
    @PutMapping("/messages/{id}") public MessageView editMessage(Authentication auth,@PathVariable UUID id,@Valid @RequestBody EditMessageRequest request) {
        UUID userId=uid(auth); GeneratedMessage updated=workflow.edit(userId,id,request.messageText()); return view(updated,contact(updated.getContactId(),userId),company(updated.getCompanyId(),userId),resume(updated.getResumeId(),userId));
    }

    @PostMapping("/messages/{id}/approve") public MessageView approve(Authentication auth,@PathVariable UUID id) {
        UUID userId=uid(auth); GeneratedMessage m=workflow.approve(userId,id); return view(m,contact(m.getContactId(),userId),company(m.getCompanyId(),userId),resume(m.getResumeId(),userId));
    }

    public record MarkSentRequest(@NotBlank @Pattern(regexp="LINKEDIN|EMAIL|REFERRAL_PORTAL|OTHER") String channel) {}
    @PostMapping("/messages/{id}/sent") @ResponseStatus(HttpStatus.CREATED)
    public MessageView markSent(Authentication auth,@PathVariable UUID id,@Valid @RequestBody MarkSentRequest request) {
        UUID userId=uid(auth); GeneratedMessage m=workflow.markSent(userId,id,request.channel()); Contact c=contact(m.getContactId(),userId);
        return view(m,c,company(m.getCompanyId(),userId),resume(m.getResumeId(),userId));
    }

    private MessageView view(GeneratedMessage m,Contact c,Company co,Resume r) {
        return new MessageView(m.getId(),c.getId(),c.getName(),c.getLinkedinUrl(),co==null?null:co.getId(),co==null?null:co.getName(),r==null?null:r.getId(),r==null?null:r.getLabel(),m.getResumeReason(),m.getRole(),m.getVariant(),m.getVersion(),m.getChannel(),m.getMessageText(),m.getRecommendationScore(),m.getRecommendationReason(),m.getStatus(),m.getCreatedAt()==null?null:OffsetDateTime.ofInstant(m.getCreatedAt(),java.time.ZoneOffset.UTC));
    }

    public record PromptView(String id, String category, int version, String promptText) {}
    public record PromptUpdate(@NotBlank @Size(max=12000) String promptText) {}
    @GetMapping("/prompts") public List<PromptView> listPrompts(Authentication auth) { return prompts.list(uid(auth)).stream().map(p->new PromptView(p.getId()==null?null:p.getId().toString(),p.getCategory(),p.getVersion(),p.getPromptText())).toList(); }
    @GetMapping("/prompts/{category}/versions") public List<PromptView> promptVersions(Authentication auth,@PathVariable String category) { return prompts.history(uid(auth),category.toUpperCase(Locale.ROOT)).stream().map(p->new PromptView(p.getId()==null?null:p.getId().toString(),p.getCategory(),p.getVersion(),p.getPromptText())).toList(); }
    @PutMapping("/prompts/{category}") public PromptView updatePrompt(Authentication auth,@PathVariable String category,@Valid @RequestBody PromptUpdate request) {
        PromptTemplate p=prompts.saveVersion(uid(auth),category.toUpperCase(Locale.ROOT),request.promptText()); return new PromptView(p.getId().toString(),p.getCategory(),p.getVersion(),p.getPromptText());
    }

    public record RuleUpdate(@NotBlank @Size(max=32) String category,@NotBlank @Size(max=100) String keyword,@Min(1) @Max(40) short weight,boolean enabled) {}
    @GetMapping("/recommendation-rules") public List<RecommendationRule> listRules(Authentication auth) { return recommendations.listRules(uid(auth)); }
    @PutMapping("/recommendation-rules") public List<RecommendationRule> updateRules(Authentication auth,@Valid @RequestBody List<@Valid RuleUpdate> updates) {
        if(updates.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST,"Keep at least one rule; you can disable every rule without removing them");
        if(updates.size()>100) throw new ApiException(HttpStatus.BAD_REQUEST,"At most 100 recommendation rules are allowed");
        return recommendations.replaceRules(uid(auth),updates.stream().map(r->new RecommendationService.RuleInput(r.category(),r.keyword(),r.weight(),r.enabled())).toList());
    }

    public record ResumeSuggestion(UUID id,String label,String reason) {}
    @GetMapping("/resume-recommendation") public ResumeSuggestion recommendResume(Authentication auth,@RequestParam UUID contactId,@RequestParam(required=false) String role) {
        UUID userId=uid(auth); Contact c=contact(contactId,userId); String target=role==null?c.getDesignation():role;
        var result=resumeRecommendations.recommend(resumes.findByUserIdAndDeletedAtIsNull(userId),target);
        return new ResumeSuggestion(result.resume()==null?null:result.resume().getId(),result.resume()==null?null:result.resume().getLabel(),result.reason());
    }

    @GetMapping("/analytics") public AnalyticsService.Analytics analytics(Authentication auth) { return analytics.calculate(uid(auth)); }
    public record ProfileUpdate(@Size(max=5000) String profileText) {}
    @GetMapping("/profile") public Map<String,String> profile(Authentication auth) { AppUser current=user(uid(auth)); return Map.of("profileText",current.getProfileText()==null?"":current.getProfileText()); }
    @PutMapping("/profile") public Map<String,String> updateProfile(Authentication auth,@Valid @RequestBody ProfileUpdate request) {
        AppUser current=user(uid(auth)); current.setProfileText(request.profileText()==null?null:request.profileText().trim()); users.save(current); return Map.of("profileText",current.getProfileText()==null?"":current.getProfileText());
    }
}
