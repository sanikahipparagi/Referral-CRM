package com.referralcrm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="resume_experiences") @Getter @Setter
public class ResumeExperience extends AuditedEntity {
    @Column(name="resume_id",nullable=false) private UUID resumeId;
    @Column(nullable=false,length=200) private String company="";
    @Column(nullable=false,length=200) private String role="";
    @Column(nullable=false,columnDefinition="text") private String description="";
    @Column(length=120) private String duration;
}
