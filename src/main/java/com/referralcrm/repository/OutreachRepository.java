package com.referralcrm.repository;
import com.referralcrm.domain.Outreach;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface OutreachRepository extends JpaRepository<Outreach, UUID>, JpaSpecificationExecutor<Outreach> {
 interface SearchRecord { String getKind(); UUID getId(); String getTitle(); String getSubtitle(); String getSnippet(); }
 @Query(value="""
   SELECT * FROM (
     SELECT 'CONTACT' AS kind, c.id AS id, c.name AS title,
       COALESCE(c.designation, '') || CASE WHEN co.name IS NOT NULL THEN ' · ' || co.name ELSE '' END AS subtitle,
       COALESCE(c.notes, '') AS snippet
     FROM contacts c LEFT JOIN companies co ON co.id=c.company_id AND co.deleted_at IS NULL
     WHERE c.user_id=:userId AND c.deleted_at IS NULL AND
       (c.name ILIKE '%' || :term || '%' OR COALESCE(c.notes,'') ILIKE '%' || :term || '%' OR COALESCE(c.designation,'') ILIKE '%' || :term || '%' OR COALESCE(c.email,'') ILIKE '%' || :term || '%')
     UNION ALL
     SELECT 'COMPANY', co.id, co.name, co.application_status, COALESCE(co.notes,'')
     FROM companies co WHERE co.user_id=:userId AND co.deleted_at IS NULL AND
       (co.name ILIKE '%' || :term || '%' OR COALESCE(co.notes,'') ILIKE '%' || :term || '%')
     UNION ALL
     SELECT 'MESSAGE', o.id, c.name || ' · ' || o.message_version, o.channel, o.message_text
     FROM outreach o JOIN contacts c ON c.id=o.contact_id AND c.deleted_at IS NULL
     WHERE o.user_id=:userId AND o.deleted_at IS NULL AND o.message_text ILIKE '%' || :term || '%'
   ) results ORDER BY kind, title LIMIT 60
   """, nativeQuery=true)
 List<SearchRecord> searchAll(@Param("userId") UUID userId, @Param("term") String term);
 @Query("select count(distinct o.contactId) from Outreach o where o.userId = :userId and o.deletedAt is null")
 long countContactedPeople(@Param("userId") UUID userId);
 long countByUserIdAndSentAtGreaterThanEqualAndSentAtLessThanAndDeletedAtIsNull(UUID userId, OffsetDateTime start, OffsetDateTime end);
 interface FollowUpRecord { UUID getId(); String getName(); String getDesignation(); String getCompanyName(); OffsetDateTime getLastSentAt(); }
 @Query(value="""
   SELECT count(*) FROM contacts c JOIN LATERAL (
     SELECT o.sent_at FROM outreach o WHERE o.contact_id = c.id AND o.user_id = :userId AND o.deleted_at IS NULL
     ORDER BY o.sent_at DESC LIMIT 1
   ) latest ON latest.sent_at <= :cutoff
   WHERE c.user_id = :userId AND c.deleted_at IS NULL AND c.status IN ('CONTACTED', 'NO_RESPONSE')
   """, nativeQuery=true)
 long countNeedsFollowUp(@Param("userId") UUID userId, @Param("cutoff") OffsetDateTime cutoff);
 @Query(value="""
   SELECT c.id AS id, c.name AS name, c.designation AS designation, co.name AS companyName, latest.sent_at AS lastSentAt
   FROM contacts c JOIN LATERAL (
     SELECT o.sent_at FROM outreach o WHERE o.contact_id = c.id AND o.user_id = :userId AND o.deleted_at IS NULL
     ORDER BY o.sent_at DESC LIMIT 1
   ) latest ON latest.sent_at <= :cutoff
   LEFT JOIN companies co ON co.id = c.company_id AND co.deleted_at IS NULL
   WHERE c.user_id = :userId AND c.deleted_at IS NULL AND c.status IN ('CONTACTED', 'NO_RESPONSE')
   ORDER BY latest.sent_at ASC LIMIT 10
   """, nativeQuery=true)
 List<FollowUpRecord> findNeedsFollowUp(@Param("userId") UUID userId, @Param("cutoff") OffsetDateTime cutoff);
}
