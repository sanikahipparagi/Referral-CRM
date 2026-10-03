package com.referralcrm.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="app_users") @Getter @Setter
public class AppUser extends AuditedEntity {
    @Column(nullable=false, unique=true, length=320) private String email;
    @Column(name="password_hash", nullable=false, length=100) private String passwordHash;
    @Column(name="full_name", nullable=false, length=160) private String fullName;
    @Column(name="profile_text", columnDefinition="text") private String profileText;
}
