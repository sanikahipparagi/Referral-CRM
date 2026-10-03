package com.referralcrm.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="resumes") @Getter @Setter
public class Resume extends AuditedEntity {
    @Column(name="user_id", nullable=false) private UUID userId;
    @Column(nullable=false, length=120) private String label;
    @Column(name="file_name", nullable=false) private String fileName;
    @Column(name="storage_key", nullable=false, length=1024) private String storageKey;
    @Column(name="content_type", nullable=false, length=120) private String contentType;
    @Column(name="file_size", nullable=false) private long fileSize;
}
