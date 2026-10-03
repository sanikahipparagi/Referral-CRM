package com.referralcrm.repository;

import com.referralcrm.domain.JobAnalysisRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobAnalysisRepository extends JpaRepository<JobAnalysisRecord,UUID> {
    List<JobAnalysisRecord> findByJobIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID jobId);
}
