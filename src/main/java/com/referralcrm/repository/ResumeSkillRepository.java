package com.referralcrm.repository;

import com.referralcrm.domain.ResumeSkill;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeSkillRepository extends JpaRepository<ResumeSkill, UUID> {
    List<ResumeSkill> findByResumeIdAndDeletedAtIsNullOrderBySkillAsc(UUID resumeId);
    List<ResumeSkill> findByResumeIdInAndDeletedAtIsNull(List<UUID> resumeIds);
    void deleteByResumeIdAndDeletedAtIsNull(UUID resumeId);
}
