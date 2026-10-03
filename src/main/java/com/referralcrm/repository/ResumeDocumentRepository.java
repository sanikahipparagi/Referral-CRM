package com.referralcrm.repository;

import com.referralcrm.domain.ResumeDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeDocumentRepository extends JpaRepository<ResumeDocument,UUID> {
    List<ResumeDocument> findByResumeIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID resumeId);
    List<ResumeDocument> findByResumeIdInAndDeletedAtIsNull(List<UUID> resumeIds);
    Optional<ResumeDocument> findByIdAndResumeIdAndDeletedAtIsNull(UUID id,UUID resumeId);
}
