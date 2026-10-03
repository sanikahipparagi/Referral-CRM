package com.referralcrm.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Stable facade for resume intelligence workflows; details remain in focused services. */
@Service
public class ResumeAnalysisService {
    private final ResumeProfileService profiles; private final ResumeExtractionService documents; private final JobAnalysisService jobs;
    public ResumeAnalysisService(ResumeProfileService profiles,ResumeExtractionService documents,JobAnalysisService jobs){this.profiles=profiles;this.documents=documents;this.jobs=jobs;}
    public ResumeProfileService.ProfileView profile(UUID userId,UUID resumeId){return documents.intelligence(userId,resumeId);}
    public JobAnalysisService.JobAnalysisResult analyze(UUID userId,UUID jobId){return jobs.analyze(userId,jobId);}
    public List<JobAnalysisService.JobAnalysisResult> history(UUID userId,UUID jobId){return jobs.history(userId,jobId);}
}
