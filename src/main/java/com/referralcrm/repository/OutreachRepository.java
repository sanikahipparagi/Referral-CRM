package com.referralcrm.repository;
import com.referralcrm.domain.Outreach;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface OutreachRepository extends JpaRepository<Outreach, UUID>, JpaSpecificationExecutor<Outreach> {}
