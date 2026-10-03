package com.referralcrm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="resume_skills") @Getter @Setter
public class ResumeSkill extends AuditedEntity {
    @Column(name="resume_id", nullable=false) private UUID resumeId;
    @Column(nullable=false, length=100) private String skill;
}
