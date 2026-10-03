package com.referralcrm.repository;

import com.referralcrm.domain.JobOpportunity;
import com.referralcrm.domain.JobStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JobOpportunityRepository extends JpaRepository<JobOpportunity, UUID>, JpaSpecificationExecutor<JobOpportunity> {
    List<JobOpportunity> findByUserIdAndDeletedAtIsNull(UUID userId);
    long countByUserIdAndStatusAndDeletedAtIsNull(UUID userId, JobStatus status);
}
