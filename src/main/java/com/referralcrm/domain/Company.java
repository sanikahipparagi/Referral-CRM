package com.referralcrm.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="companies") @Getter @Setter
public class Company extends AuditedEntity {
    @Column(name="user_id", nullable=false) private java.util.UUID userId;
    @Column(nullable=false, length=200) private String name;
    @Column(name="career_page", length=2048) private String careerPage;
    @Column(nullable=false) private short priority = 3;
    @Column(name="dream_company", nullable=false) private boolean dreamCompany;
    @Column(name="application_status", nullable=false, length=32) private String applicationStatus = "NOT_APPLIED";
    @Column(columnDefinition="text") private String notes;
}
