package com.referralcrm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="recommendation_rules") @Getter @Setter
public class RecommendationRule extends AuditedEntity {
    @Column(name="user_id") private UUID userId;
    @Column(nullable=false, length=32) private String category;
    @Column(nullable=false, length=100) private String keyword;
    @Column(nullable=false) private short weight;
    @Column(nullable=false) private boolean enabled = true;
}
