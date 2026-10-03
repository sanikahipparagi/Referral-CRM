package com.referralcrm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="generated_messages") @Getter @Setter
public class GeneratedMessage extends AuditedEntity {
    @Column(name="user_id", nullable=false) private UUID userId;
    @Column(name="contact_id", nullable=false) private UUID contactId;
    @Column(name="company_id") private UUID companyId;
    @Column(name="resume_id") private UUID resumeId;
    @Column(length=200) private String role;
    @Column(nullable=false, length=32) private String variant;
    @Column(nullable=false) private int version;
    @Column(nullable=false, length=32) private String channel;
    @Column(name="message_text", nullable=false, columnDefinition="text") private String messageText;
    @Column(name="recommendation_score", nullable=false) private short recommendationScore;
    @Column(name="recommendation_reason", columnDefinition="text") private String recommendationReason;
    @Column(name="resume_reason", columnDefinition="text") private String resumeReason;
    @Column(nullable=false, length=16) private String status = "DRAFT";
    @Column(name="approved_at") private OffsetDateTime approvedAt;
    @Column(name="sent_at") private OffsetDateTime sentAt;
}
