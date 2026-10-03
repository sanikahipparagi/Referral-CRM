package com.referralcrm.service;

import com.referralcrm.api.ApiException;
import com.referralcrm.domain.*;
import com.referralcrm.repository.CompanyRepository;
import com.referralcrm.repository.JobOpportunityRepository;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {
    public record JobData(UUID companyId, String title, String description, String location, String employmentType, String experienceLevel,
                          String source, String careerUrl, String salaryRange, java.util.List<String> skills, JobStatus status, JobPriority priority) {}
    private final JobOpportunityRepository jobs;
    private final CompanyRepository companies;
    public JobService(JobOpportunityRepository jobs, CompanyRepository companies) { this.jobs=jobs; this.companies=companies; }

    public Page<JobOpportunity> list(UUID userId, String search, JobStatus status, UUID companyId, JobPriority priority, Pageable page) {
        Specification<JobOpportunity> spec=(root,q,cb)->cb.and(cb.equal(root.get("userId"),userId),cb.isNull(root.get("deletedAt")));
        if (status!=null) spec=spec.and((root,q,cb)->cb.equal(root.get("status"),status));
        if (companyId!=null) spec=spec.and((root,q,cb)->cb.equal(root.get("companyId"),companyId));
        if (priority!=null) spec=spec.and((root,q,cb)->cb.equal(root.get("priority"),priority));
        if (search!=null&&!search.isBlank()) { String like="%"+search.toLowerCase(Locale.ROOT)+"%"; spec=spec.and((root,q,cb)->cb.or(cb.like(cb.lower(root.get("title")),like),cb.like(cb.lower(root.get("description")),like),cb.like(cb.lower(root.get("location")),like),cb.like(cb.lower(root.get("source")),like))); }
        return jobs.findAll(spec,page);
    }
    public JobOpportunity get(UUID userId, UUID id) { return owned(userId,id); }
    @Transactional public JobOpportunity create(UUID userId, JobData request) {
        JobOpportunity job=new JobOpportunity(); job.setUserId(userId); apply(job,request,userId); return jobs.save(job);
    }
    @Transactional public JobOpportunity update(UUID userId, UUID id, JobData request) {
        JobOpportunity job=owned(userId,id); apply(job,request,userId); return jobs.save(job);
    }
    @Transactional public void delete(UUID userId, UUID id) { JobOpportunity job=owned(userId,id); job.setDeletedAt(OffsetDateTime.now()); jobs.save(job); }

    public JobOpportunity owned(UUID userId, UUID id) {
        JobOpportunity job=jobs.findById(id).filter(j->j.getDeletedAt()==null&&j.getUserId().equals(userId)).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Job not found"));
        return job;
    }
    private void apply(JobOpportunity job, JobData request, UUID userId) {
        companies.findById(request.companyId()).filter(c->c.getDeletedAt()==null&&c.getUserId().equals(userId)).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Company not found"));
        String title=request.title().trim(),description=request.description().trim();
        java.util.Set<String> requestedSkills=request.skills()==null?new java.util.LinkedHashSet<>():request.skills().stream().filter(s->s!=null&&!s.isBlank()).map(String::trim).collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        boolean matchInputsChanged=job.getId()!=null&&(!Objects.equals(job.getTitle(),title)||!Objects.equals(job.getDescription(),description)||!Objects.equals(job.getSkills(),requestedSkills));
        job.setCompanyId(request.companyId()); job.setTitle(title); job.setDescription(description);
        job.setLocation(blankToNull(request.location())); job.setEmploymentType(blankToNull(request.employmentType())); job.setExperienceLevel(blankToNull(request.experienceLevel()));
        job.setSource(blankToNull(request.source())); job.setCareerUrl(blankToNull(request.careerUrl())); job.setSalaryRange(blankToNull(request.salaryRange()));
        job.setSkills(requestedSkills);
        job.setStatus(request.status()==null?JobStatus.FOUND:request.status()); job.setPriority(request.priority()==null?JobPriority.MEDIUM:request.priority());
        if(matchInputsChanged) job.setMatchScore((short)0);
    }
    private String blankToNull(String value) { return value==null||value.isBlank()?null:value.trim(); }
}
