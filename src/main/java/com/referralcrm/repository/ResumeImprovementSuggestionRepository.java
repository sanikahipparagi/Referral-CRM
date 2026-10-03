package com.referralcrm.repository;

import com.referralcrm.domain.ResumeImprovementSuggestion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeImprovementSuggestionRepository extends JpaRepository<ResumeImprovementSuggestion,UUID> {
    List<ResumeImprovementSuggestion> findByResumeIdAndJobIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID resumeId,UUID jobId);
}
