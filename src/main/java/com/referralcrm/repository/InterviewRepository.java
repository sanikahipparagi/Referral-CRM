package com.referralcrm.repository;
import com.referralcrm.domain.Interview;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
public interface InterviewRepository extends JpaRepository<Interview, UUID>, JpaSpecificationExecutor<Interview> {
 long countByUserIdAndDeletedAtIsNull(UUID userId);
 List<Interview> findByUserIdAndDeletedAtIsNull(UUID userId);
}
