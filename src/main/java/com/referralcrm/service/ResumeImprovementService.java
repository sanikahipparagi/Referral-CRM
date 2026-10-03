package com.referralcrm.service;

import com.referralcrm.api.ApiException;
import com.referralcrm.domain.*;
import com.referralcrm.repository.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumeImprovementService {
    public record SuggestionView(UUID id,UUID resumeId,UUID jobId,String suggestion,SuggestionStatus status) {}
    private final ResumeImprovementSuggestionRepository suggestions; private final ResumeRepository resumes; private final JobService jobs;
    public ResumeImprovementService(ResumeImprovementSuggestionRepository suggestions,ResumeRepository resumes,JobService jobs){this.suggestions=suggestions;this.resumes=resumes;this.jobs=jobs;}
    public List<SuggestionView> list(UUID userId,UUID jobId,UUID resumeId) { jobs.get(userId,jobId); ownedResume(userId,resumeId); return suggestions.findByResumeIdAndJobIdAndDeletedAtIsNullOrderByCreatedAtDesc(resumeId,jobId).stream().map(this::view).toList(); }
    @Transactional public List<SuggestionView> replacePending(UUID userId,UUID jobId,UUID resumeId,List<String> requested) {
        jobs.get(userId,jobId); ownedResume(userId,resumeId);
        List<ResumeImprovementSuggestion> active=suggestions.findByResumeIdAndJobIdAndDeletedAtIsNullOrderByCreatedAtDesc(resumeId,jobId);
        active.stream().filter(s->s.getStatus()==SuggestionStatus.PENDING).forEach(s->s.setDeletedAt(OffsetDateTime.now())); suggestions.saveAll(active);
        return suggestions.saveAll(requested.stream().distinct().map(text->{ResumeImprovementSuggestion s=new ResumeImprovementSuggestion();s.setResumeId(resumeId);s.setJobId(jobId);s.setSuggestion(text);s.setStatus(SuggestionStatus.PENDING);return s;}).toList()).stream().map(this::view).toList();
    }
    @Transactional public SuggestionView updateStatus(UUID userId,UUID jobId,UUID suggestionId,SuggestionStatus status) {
        jobs.get(userId,jobId);
        ResumeImprovementSuggestion suggestion=suggestions.findById(suggestionId).filter(s->s.getDeletedAt()==null&&s.getJobId().equals(jobId)).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Suggestion not found"));
        ownedResume(userId,suggestion.getResumeId()); suggestion.setStatus(status); return view(suggestions.save(suggestion));
    }
    private Resume ownedResume(UUID userId,UUID id) { return resumes.findById(id).filter(r->r.getUserId().equals(userId)&&r.getDeletedAt()==null).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Resume not found")); }
    private SuggestionView view(ResumeImprovementSuggestion s){return new SuggestionView(s.getId(),s.getResumeId(),s.getJobId(),s.getSuggestion(),s.getStatus());}
}
