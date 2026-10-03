package com.referralcrm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="job_analysis_results") @Getter @Setter
public class JobAnalysisRecord extends AuditedEntity {
    @Column(name="job_id",nullable=false) private UUID jobId;
    @Column(name="resume_id") private UUID resumeId;
    @Column(name="match_score",nullable=false) private short matchScore;
    @Column(name="result_json",nullable=false,columnDefinition="text") private String resultJson;
}
