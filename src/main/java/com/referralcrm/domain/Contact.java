package com.referralcrm.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="contacts") @Getter @Setter
public class Contact extends AuditedEntity {
    @Column(name="user_id", nullable=false) private UUID userId;
    @Column(name="company_id") private UUID companyId;
    @Column(nullable=false, length=160) private String name;
    @Column(name="linkedin_url", length=2048) private String linkedinUrl;
    @Column(length=200) private String designation;
    @Column(length=200) private String location;
    @Column(length=320) private String email;
    @Column(length=120) private String source;
    @Column(name="date_added", nullable=false) private LocalDate dateAdded = LocalDate.now();
    @Column(columnDefinition="text") private String notes;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=32) private ContactStatus status = ContactStatus.NOT_CONTACTED;
    @Column(name="replied_at") private OffsetDateTime repliedAt;
}
