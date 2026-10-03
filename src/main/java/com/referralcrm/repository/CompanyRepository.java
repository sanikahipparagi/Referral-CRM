package com.referralcrm.repository;
import com.referralcrm.domain.Company;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;
public interface CompanyRepository extends JpaRepository<Company, UUID>, JpaSpecificationExecutor<Company> {
 interface CompanyStats { UUID getId(); String getName(); long getContactCount(); long getOutreachCount(); long getReferralCount(); }
 @Query(value="""
   SELECT co.id AS id, co.name AS name, COUNT(DISTINCT c.id) AS "contactCount",
     COUNT(DISTINCT o.id) AS "outreachCount",
     COUNT(DISTINCT CASE WHEN c.status = 'REFERRED' THEN c.id END) AS "referralCount"
   FROM companies co LEFT JOIN contacts c ON c.company_id = co.id AND c.deleted_at IS NULL
   LEFT JOIN outreach o ON o.contact_id = c.id AND o.deleted_at IS NULL
   WHERE co.user_id = :userId AND co.deleted_at IS NULL
   GROUP BY co.id, co.name ORDER BY "outreachCount" DESC, "contactCount" DESC, co.name ASC LIMIT 8
   """, nativeQuery=true)
 List<CompanyStats> dashboardStats(@Param("userId") UUID userId);
 List<com.referralcrm.domain.Company> findByUserIdAndDeletedAtIsNull(UUID userId);
}
