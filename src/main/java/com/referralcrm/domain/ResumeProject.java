package com.referralcrm.domain;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="resume_projects") @Getter @Setter
public class ResumeProject extends AuditedEntity {
    @Column(name="resume_id",nullable=false) private UUID resumeId;
    @Column(nullable=false,length=200) private String name;
    @Column(nullable=false,columnDefinition="text") private String description="";
    @ElementCollection(fetch=FetchType.EAGER)
    @CollectionTable(name="resume_project_technologies",joinColumns=@JoinColumn(name="project_id"))
    @Column(name="technology",nullable=false,length=100)
    private Set<String> technologies=new LinkedHashSet<>();
}
