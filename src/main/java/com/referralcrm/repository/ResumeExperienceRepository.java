package com.referralcrm.repository;

import com.referralcrm.domain.ResumeExperience;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeExperienceRepository extends JpaRepository<ResumeExperience,UUID> {
    List<ResumeExperience> findByResumeIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID resumeId);
    List<ResumeExperience> findByResumeIdInAndDeletedAtIsNull(List<UUID> resumeIds);
}
