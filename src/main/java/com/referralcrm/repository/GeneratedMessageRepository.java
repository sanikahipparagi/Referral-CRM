package com.referralcrm.repository;

import com.referralcrm.domain.GeneratedMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneratedMessageRepository extends JpaRepository<GeneratedMessage, UUID> {
    List<GeneratedMessage> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);
    List<GeneratedMessage> findByUserIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId, String status);
    Optional<GeneratedMessage> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);
    Optional<GeneratedMessage> findFirstByContactIdAndVariantAndDeletedAtIsNullOrderByVersionDesc(UUID contactId, String variant);
}
