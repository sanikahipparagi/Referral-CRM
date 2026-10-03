package com.referralcrm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="prompt_templates") @Getter @Setter
public class PromptTemplate extends AuditedEntity {
    @Column(name="user_id") private UUID userId;
    @Column(nullable=false, length=32) private String category;
    @Column(nullable=false) private int version;
    @Column(name="prompt_text", nullable=false, columnDefinition="text") private String promptText;
    @Column(nullable=false) private boolean active = true;
}
