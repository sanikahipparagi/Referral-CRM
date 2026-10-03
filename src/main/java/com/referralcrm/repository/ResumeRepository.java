package com.referralcrm.repository;
import com.referralcrm.domain.Resume;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface ResumeRepository extends JpaRepository<Resume, UUID>, JpaSpecificationExecutor<Resume> {}
