package com.referralcrm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="resume_improvement_suggestions") @Getter @Setter
public class ResumeImprovementSuggestion extends AuditedEntity {
    @Column(name="resume_id",nullable=false) private UUID resumeId;
    @Column(name="job_id",nullable=false) private UUID jobId;
    @Column(nullable=false,columnDefinition="text") private String suggestion;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private SuggestionStatus status=SuggestionStatus.PENDING;
}
