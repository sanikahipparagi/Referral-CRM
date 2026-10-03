package com.referralcrm.domain;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="job_opportunities")
@Getter @Setter
public class JobOpportunity extends AuditedEntity {
    @Column(name="user_id", nullable=false) private UUID userId;
    @Column(name="company_id", nullable=false) private UUID companyId;
    @Column(nullable=false, length=200) private String title;
    @Column(nullable=false, columnDefinition="text") private String description;
    @Column(length=200) private String location;
    @Column(name="employment_type", length=32) private String employmentType;
    @Column(name="experience_level", length=32) private String experienceLevel;
    @Column(length=120) private String source;
    @Column(name="career_url", length=2048) private String careerUrl;
    @Column(name="salary_range", length=120) private String salaryRange;
    @ElementCollection(fetch=FetchType.EAGER)
    @CollectionTable(name="job_skills", joinColumns=@JoinColumn(name="job_id"))
    @Column(name="skill", nullable=false, length=100)
    private Set<String> skills = new LinkedHashSet<>();
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=32) private JobStatus status = JobStatus.FOUND;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=16) private JobPriority priority = JobPriority.MEDIUM;
    @Column(name="match_score", nullable=false) private short matchScore;
}
