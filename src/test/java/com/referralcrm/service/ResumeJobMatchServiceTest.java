package com.referralcrm.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.referralcrm.domain.JobOpportunity;
import com.referralcrm.domain.Resume;
import com.referralcrm.domain.ResumeSkill;
import com.referralcrm.repository.*;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResumeJobMatchServiceTest {
    @Test void comparesEvidenceRanksResumesAndExplainsMissingSkills() {
        UUID userId=UUID.randomUUID();
        Resume backend=resume(userId,"Java Backend Resume"); Resume general=resume(userId,"General Resume");
        ResumeRepository resumes=mock(ResumeRepository.class); ResumeSkillRepository skills=mock(ResumeSkillRepository.class);
        ResumeDocumentRepository documents=mock(ResumeDocumentRepository.class); ResumeProjectRepository projects=mock(ResumeProjectRepository.class);
        ResumeExperienceRepository experiences=mock(ResumeExperienceRepository.class); ResumeProfileService profile=mock(ResumeProfileService.class);
        when(resumes.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(List.of(backend,general));
        when(skills.findByResumeIdInAndDeletedAtIsNull(any())).thenReturn(List.of(skill(backend.getId(),"Java"),skill(backend.getId(),"Spring Boot"),skill(general.getId(),"Java")));
        when(documents.findByResumeIdInAndDeletedAtIsNull(any())).thenReturn(List.of());
        when(projects.findByResumeIdInAndDeletedAtIsNull(any())).thenReturn(List.of());
        when(experiences.findByResumeIdInAndDeletedAtIsNull(any())).thenReturn(List.of());
        when(profile.extractSkills(anyString())).thenReturn(List.of());
        ResumeJobMatchService service=new ResumeJobMatchService(resumes,skills,documents,projects,experiences,profile);
        JobOpportunity job=new JobOpportunity(); job.setTitle("Backend Engineer"); job.setSkills(new LinkedHashSet<>(List.of("Java","Spring Boot","AWS")));

        ResumeJobMatchService.MatchResult result=service.match(userId,job);

        assertEquals(backend.getId(),result.recommendedResumeId());
        assertEquals("Java Backend Resume",result.recommendedResume());
        assertEquals(List.of("Java","Spring Boot"),result.matchingSkills());
        assertEquals(List.of("AWS"),result.missingSkills());
        assertEquals(51,result.score());
        assertTrue(result.resumeReason().contains("2 of 3"));
        assertTrue(result.recommendations().stream().anyMatch(text->text.contains("AWS")));
    }

    private Resume resume(UUID userId,String label) { Resume r=new Resume(); r.setId(UUID.randomUUID()); r.setUserId(userId); r.setLabel(label); return r; }
    private ResumeSkill skill(UUID resumeId,String name) { ResumeSkill s=new ResumeSkill(); s.setResumeId(resumeId); s.setSkill(name); return s; }
}
