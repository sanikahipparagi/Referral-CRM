package com.referralcrm.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.referralcrm.domain.JobAnalysisRecord;
import com.referralcrm.domain.JobOpportunity;
import com.referralcrm.repository.JobAnalysisRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class JobAnalysisServiceTest {
    @Test void analyzesJobStoresGroundedSuggestionsAndMakesResultAvailableInHistory() {
        UUID userId=UUID.randomUUID(),jobId=UUID.randomUUID(),resumeId=UUID.randomUUID(),analysisId=UUID.randomUUID();
        JobOpportunity job=new JobOpportunity(); job.setId(jobId); job.setTitle("Backend Engineer"); job.setDescription("Build payment services.");
        JobService jobs=mock(JobService.class); when(jobs.get(userId,jobId)).thenReturn(job);
        ResumeJobMatchService matching=mock(ResumeJobMatchService.class);
        when(matching.match(userId,job)).thenReturn(new ResumeJobMatchService.MatchResult(88,List.of("Java","AWS"),List.of("Kafka"),75,
            List.of("Payment platform project demonstrates Java."),List.of("Kafka is not present in the resume."),List.of("Highlight payment system work."),
            resumeId,"Java Backend Resume","Strong evidence in 2 of 3 skills.","Built payment platform with Java and AWS."));
        ResumeImprovementService.SuggestionView suggestion=new ResumeImprovementService.SuggestionView(UUID.randomUUID(),resumeId,jobId,"If accurate, include Kafka evidence.",com.referralcrm.domain.SuggestionStatus.PENDING);
        ResumeImprovementService improvements=mock(ResumeImprovementService.class);
        when(improvements.replacePending(eq(userId),eq(jobId),eq(resumeId),anyList())).thenReturn(List.of(suggestion));
        JobAnalysisRepository analyses=mock(JobAnalysisRepository.class);
        AtomicReference<JobAnalysisRecord> stored=new AtomicReference<>();
        when(analyses.save(any())).thenAnswer(invocation->{JobAnalysisRecord row=invocation.getArgument(0);row.setId(analysisId);stored.set(row);return row;});
        when(analyses.findByJobIdAndDeletedAtIsNullOrderByCreatedAtDesc(jobId)).thenAnswer(invocation->List.of(stored.get()));
        LLMAnalysisService llm=mock(LLMAnalysisService.class); when(llm.analyze(anyString(),anyMap())).thenReturn(Optional.empty());
        JobAnalysisService service=new JobAnalysisService(jobs,matching,improvements,analyses,new ObjectMapper(),llm);

        JobAnalysisService.JobAnalysisResult result=service.analyze(userId,jobId);

        assertEquals(analysisId,result.analysisId()); assertEquals(88,result.matchScore());
        assertEquals(List.of("Java","AWS"),result.matchingSkills()); assertEquals(List.of("Kafka"),result.missingSkills());
        assertEquals(resumeId,result.recommendedResumeId()); assertEquals("Java Backend Resume",result.recommendedResume());
        assertEquals(List.of(suggestion),result.suggestions()); assertEquals("",result.optionalAiInsight());
        verify(improvements).replacePending(eq(userId),eq(jobId),eq(resumeId),argThat(values->values.stream().anyMatch(v->v.contains("Kafka"))));
        assertEquals(analysisId,service.history(userId,jobId).getFirst().analysisId());
    }
}
