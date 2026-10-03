package com.referralcrm.repository;

import com.referralcrm.domain.ResumeProject;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeProjectRepository extends JpaRepository<ResumeProject,UUID> {
    List<ResumeProject> findByResumeIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID resumeId);
    List<ResumeProject> findByResumeIdInAndDeletedAtIsNull(List<UUID> resumeIds);
}
