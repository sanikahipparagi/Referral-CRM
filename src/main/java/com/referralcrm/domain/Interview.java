package com.referralcrm.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="interviews") @Getter @Setter
public class Interview extends AuditedEntity {
    @Column(name="user_id", nullable=false) private UUID userId;
    @Column(name="company_id", nullable=false) private UUID companyId;
    @Column(nullable=false, length=160) private String round;
    @Column(name="interview_at") private OffsetDateTime interviewAt;
    @Column(nullable=false, length=32) private String result = "PENDING";
    @Column(columnDefinition="text") private String feedback;
    @Column(name="package_amount", precision=14, scale=2) private BigDecimal packageAmount;
    @Column(columnDefinition="text") private String notes;
}
