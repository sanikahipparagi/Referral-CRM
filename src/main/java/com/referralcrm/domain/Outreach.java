package com.referralcrm.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="outreach") @Getter @Setter
public class Outreach extends AuditedEntity {
    @Column(name="user_id", nullable=false) private UUID userId;
    @Column(name="contact_id", nullable=false) private UUID contactId;
    @Column(name="resume_id") private UUID resumeId;
    @Column(nullable=false, length=32) private String channel;
    @Column(name="message_version", nullable=false, length=32) private String messageVersion;
    @Column(name="message_text", nullable=false, columnDefinition="text") private String messageText;
    @Column(name="sent_at", nullable=false) private OffsetDateTime sentAt;
}
