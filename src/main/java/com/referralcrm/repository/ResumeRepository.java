package com.referralcrm.repository;
import com.referralcrm.domain.Resume;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
public interface ResumeRepository extends JpaRepository<Resume, UUID>, JpaSpecificationExecutor<Resume> {
 List<Resume> findByUserIdAndDeletedAtIsNull(UUID userId);
}
