package com.referralcrm.repository;
import com.referralcrm.domain.Contact;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
public interface ContactRepository extends JpaRepository<Contact, UUID>, JpaSpecificationExecutor<Contact> {
 long countByUserIdAndStatusAndDeletedAtIsNull(UUID userId, com.referralcrm.domain.ContactStatus status);
 List<Contact> findByUserIdAndDeletedAtIsNull(UUID userId);
}
