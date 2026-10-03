package com.referralcrm.repository;

import com.referralcrm.domain.PromptTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, UUID> {
    Optional<PromptTemplate> findFirstByUserIdAndCategoryAndActiveTrueAndDeletedAtIsNullOrderByVersionDesc(UUID userId, String category);
    Optional<PromptTemplate> findFirstByUserIdIsNullAndCategoryAndDeletedAtIsNullOrderByVersionDesc(String category);
    List<PromptTemplate> findByUserIdAndDeletedAtIsNullOrderByCategoryAscVersionDesc(UUID userId);
    List<PromptTemplate> findByUserIdIsNullAndDeletedAtIsNullOrderByCategoryAscVersionDesc();
    Optional<PromptTemplate> findFirstByUserIdAndCategoryOrderByVersionDesc(UUID userId, String category);
    List<PromptTemplate> findByUserIdAndCategoryAndDeletedAtIsNullOrderByVersionDesc(UUID userId, String category);
    List<PromptTemplate> findByUserIdIsNullAndCategoryAndDeletedAtIsNullOrderByVersionDesc(String category);
}
