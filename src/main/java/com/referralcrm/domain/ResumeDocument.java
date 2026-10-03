package com.referralcrm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="resume_documents") @Getter @Setter
public class ResumeDocument extends AuditedEntity {
    @Column(name="resume_id",nullable=false) private UUID resumeId;
    @Column(name="file_name",nullable=false,length=255) private String fileName;
    @Column(name="file_type",nullable=false,length=120) private String fileType;
    @Column(name="file_size",nullable=false) private long fileSize;
    @Column(name="storage_path",nullable=false,length=1024) private String storagePath;
    @Column(name="extracted_text",nullable=false,columnDefinition="text") private String extractedText="";
}
