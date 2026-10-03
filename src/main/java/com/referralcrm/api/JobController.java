package com.referralcrm.api;

import com.referralcrm.domain.*;
import com.referralcrm.service.JobService;
import com.referralcrm.service.JobMatchingService;
import com.referralcrm.service.OpportunityRankingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobs")
@Validated
public class JobController {
    private final JobService jobs;
    private final JobMatchingService matching;
    private final OpportunityRankingService ranking;
    public JobController(JobService jobs,JobMatchingService matching,OpportunityRankingService ranking) { this.jobs=jobs; this.matching=matching; this.ranking=ranking; }
    private UUID uid(Authentication auth) { return (UUID)auth.getPrincipal(); }

    public record JobRequest(@NotNull UUID companyId, @NotBlank @Size(max=200) String title, @NotBlank @Size(max=50000) String description,
        @Size(max=200) String location, @Size(max=32) String employmentType, @Size(max=32) String experienceLevel,
        @Size(max=120) String source, @Size(max=2048) @Pattern(regexp="^$|https?://.*", message="must be an http(s) URL") String careerUrl,
        @Size(max=120) String salaryRange, @Size(max=50) List<@NotBlank @Size(max=100) String> skills,
        JobStatus status, JobPriority priority) {}
    public record JobView(UUID id, UUID companyId, String title, String description, String location, String employmentType,
        String experienceLevel, String source, String careerUrl, String salaryRange, Set<String> skills, JobStatus status,
        JobPriority priority, int matchScore, java.time.Instant createdAt, java.time.Instant updatedAt) {
        static JobView from(JobOpportunity job) { return new JobView(job.getId(),job.getCompanyId(),job.getTitle(),job.getDescription(),job.getLocation(),job.getEmploymentType(),job.getExperienceLevel(),job.getSource(),job.getCareerUrl(),job.getSalaryRange(),job.getSkills(),job.getStatus(),job.getPriority(),job.getMatchScore(),job.getCreatedAt(),job.getUpdatedAt()); }
    }
    private JobService.JobData data(JobRequest r) { return new JobService.JobData(r.companyId(),r.title(),r.description(),r.location(),r.employmentType(),r.experienceLevel(),r.source(),r.careerUrl(),r.salaryRange(),r.skills(),r.status(),r.priority()); }

    @GetMapping public Page<JobView> list(Authentication auth, @RequestParam(defaultValue="0") @Min(0) int page,
        @RequestParam(defaultValue="20") @Min(1) @Max(100) int size, @RequestParam(defaultValue="createdAt") String sort,
        @RequestParam(defaultValue="desc") String direction, @RequestParam(required=false) String search,
        @RequestParam(required=false) JobStatus status, @RequestParam(required=false) UUID companyId,
        @RequestParam(required=false) JobPriority priority) {
        Set<String> allowed=Set.of("createdAt","updatedAt","title","status","priority","matchScore","location");
        if(!allowed.contains(sort)) throw new ApiException(HttpStatus.BAD_REQUEST,"Unsupported sort field");
        Sort order=Sort.by("asc".equalsIgnoreCase(direction)?Sort.Direction.ASC:Sort.Direction.DESC,sort);
        return jobs.list(uid(auth),search,status,companyId,priority,PageRequest.of(page,size,order)).map(JobView::from);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public JobView create(Authentication auth,@Valid @RequestBody JobRequest request) { return JobView.from(jobs.create(uid(auth),data(request))); }
    @GetMapping("/{id}") public JobView get(Authentication auth,@PathVariable UUID id) { return JobView.from(jobs.get(uid(auth),id)); }
    @PutMapping("/{id}") public JobView update(Authentication auth,@PathVariable UUID id,@Valid @RequestBody JobRequest request) { return JobView.from(jobs.update(uid(auth),id,data(request))); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(Authentication auth,@PathVariable UUID id) { jobs.delete(uid(auth),id); }
    @GetMapping("/{id}/match") public JobMatchingService.MatchResult match(Authentication auth,@PathVariable UUID id) { return matching.match(uid(auth),jobs.get(uid(auth),id)); }
    @GetMapping("/top") public List<OpportunityRankingService.RankedOpportunity> top(Authentication auth,@RequestParam(defaultValue="10") @Min(1) @Max(50) int limit) { return ranking.top(uid(auth),limit); }
}
