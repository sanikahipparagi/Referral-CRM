package com.referralcrm.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.referralcrm.domain.*;
import com.referralcrm.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class JobIntelligenceServicesTest {
    @Test void jobServiceCreatesUpdatesListsAndSoftDeletesOwnedJobs() {
        UUID userId=UUID.randomUUID(),companyId=UUID.randomUUID(); Company company=new Company(); company.setId(companyId); company.setUserId(userId);
        CompanyRepository companies=mock(CompanyRepository.class); JobOpportunityRepository repository=mock(JobOpportunityRepository.class);
        when(companies.findById(companyId)).thenReturn(Optional.of(company)); when(repository.save(any())).thenAnswer(inv->inv.getArgument(0));
        JobService service=new JobService(repository,companies);
        JobService.JobData data=new JobService.JobData(companyId,"Backend Engineer","Build Java services",null,"FULL_TIME","SENIOR",null,null,null,List.of("Java"),null,null);
        JobOpportunity created=service.create(userId,data); created.setId(UUID.randomUUID());
        when(repository.findById(created.getId())).thenReturn(Optional.of(created));
        assertEquals(JobStatus.FOUND,created.getStatus()); assertEquals(JobPriority.MEDIUM,created.getPriority());
        created.setMatchScore((short)80);
        JobService.JobData updated=new JobService.JobData(companyId,"Staff Backend Engineer","Build scalable Java services",null,null,null,null,null,null,List.of("Java","AWS"),JobStatus.INTERESTED,JobPriority.HIGH);
        assertEquals("Staff Backend Engineer",service.update(userId,created.getId(),updated).getTitle()); assertEquals(0,created.getMatchScore());
        service.delete(userId,created.getId()); assertNotNull(created.getDeletedAt());
        verify(repository,times(3)).save(any());
    }

    @Test void matchingReportsMissingSkillsAndRecommendsTheResumeWithBestCoverage() {
        UUID userId=UUID.randomUUID(),resumeId=UUID.randomUUID();
        JobOpportunityRepository jobs=mock(JobOpportunityRepository.class); AppUserRepository users=mock(AppUserRepository.class);
        ResumeRepository resumes=mock(ResumeRepository.class); ResumeSkillRepository skills=mock(ResumeSkillRepository.class);
        Resume resume=new Resume(); resume.setId(resumeId); resume.setLabel("Java Backend"); resume.setFileName("java.pdf");
        ResumeSkill java=skill(resumeId,"Java"),spring=skill(resumeId,"Spring Boot");
        AppUser user=new AppUser(); user.setProfileText("Backend engineer with Java, Spring Boot, and AWS experience");
        when(users.findById(userId)).thenReturn(Optional.of(user)); when(resumes.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(List.of(resume));
        when(skills.findByResumeIdInAndDeletedAtIsNull(List.of(resumeId))).thenReturn(List.of(java,spring)); when(jobs.save(any())).thenAnswer(inv->inv.getArgument(0));
        JobMatchingService service=new JobMatchingService(jobs,users,resumes,skills,new ResumeRecommendationService());
        JobOpportunity job=new JobOpportunity(); job.setId(UUID.randomUUID()); job.setUserId(userId); job.setTitle("Senior Backend Engineer"); job.setDescription("Java Spring AWS Kafka"); job.setSkills(new LinkedHashSet<>(List.of("Java","Spring Boot","AWS","Kafka")));

        JobMatchingService.MatchResult result=service.match(userId,job);

        assertEquals(List.of("Java","Spring Boot","AWS"),result.matchingSkills()); assertEquals(List.of("Kafka"),result.missingSkills());
        assertEquals(resumeId,result.recommendedResumeId()); assertTrue(result.score()>=60); assertEquals(result.score(),job.getMatchScore());
    }

    @Test void rankingUsesDreamCompanyPriorityAndReturnsHighestWeightedOpportunityFirst() {
        UUID userId=UUID.randomUUID(),companyId=UUID.randomUUID();
        JobOpportunity first=new JobOpportunity(); first.setId(UUID.randomUUID()); first.setUserId(userId); first.setCompanyId(companyId); first.setTitle("Java Backend Engineer"); first.setStatus(JobStatus.FOUND);
        JobOpportunity second=new JobOpportunity(); second.setId(UUID.randomUUID()); second.setUserId(userId); second.setCompanyId(companyId); second.setTitle("Unrelated Role"); second.setStatus(JobStatus.FOUND);
        JobOpportunityRepository jobs=mock(JobOpportunityRepository.class); CompanyRepository companies=mock(CompanyRepository.class); AppUserRepository users=mock(AppUserRepository.class); JobMatchingService matching=mock(JobMatchingService.class);
        Company company=new Company(); company.setId(companyId); company.setUserId(userId); company.setDreamCompany(true); company.setPriority((short)5);
        when(jobs.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(List.of(first,second)); when(companies.findById(companyId)).thenReturn(Optional.of(company));
        when(users.findById(userId)).thenReturn(Optional.of(profile("Java Backend Engineer with Spring Boot")));
        when(matching.calculate(userId,first)).thenReturn(match(90)); when(matching.calculate(userId,second)).thenReturn(match(10));

        var result=new OpportunityRankingService(jobs,companies,users,matching).top(userId,5);

        assertEquals(first.getId(),result.getFirst().id()); assertEquals(76,result.getFirst().rankingScore()); assertTrue(result.getFirst().reasons().contains("Target company"));
    }

    private ResumeSkill skill(UUID resumeId,String name) { ResumeSkill s=new ResumeSkill(); s.setResumeId(resumeId); s.setSkill(name); return s; }
    private AppUser profile(String text) { AppUser u=new AppUser(); u.setProfileText(text); return u; }
    private JobMatchingService.MatchResult match(int score) { return new JobMatchingService.MatchResult(score,List.of("Java"),List.of("Kafka"),"Strong",null,null,"No resume"); }
}
