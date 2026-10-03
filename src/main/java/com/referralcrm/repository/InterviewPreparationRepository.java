package com.referralcrm.repository;

import com.referralcrm.domain.InterviewPreparationNote;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewPreparationRepository extends JpaRepository<InterviewPreparationNote,UUID> {
    List<InterviewPreparationNote> findByJobIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID jobId);
    Optional<InterviewPreparationNote> findByIdAndJobIdAndDeletedAtIsNull(UUID id,UUID jobId);
}
