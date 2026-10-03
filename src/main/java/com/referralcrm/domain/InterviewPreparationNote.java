package com.referralcrm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="interview_preparation_notes") @Getter @Setter
public class InterviewPreparationNote extends AuditedEntity {
    @Column(name="job_id",nullable=false) private UUID jobId;
    @Column(name="resume_id") private UUID resumeId;
    @Column(name="technical_topics",nullable=false,columnDefinition="text") private String technicalTopics="[]";
    @Column(name="system_design_topics",nullable=false,columnDefinition="text") private String systemDesignTopics="[]";
    @Column(name="possible_questions",nullable=false,columnDefinition="text") private String possibleQuestions="[]";
    @Column(nullable=false,columnDefinition="text") private String notes="";
}
