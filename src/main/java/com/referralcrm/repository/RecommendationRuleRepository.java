package com.referralcrm.repository;

import com.referralcrm.domain.RecommendationRule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRuleRepository extends JpaRepository<RecommendationRule, UUID> {
    List<RecommendationRule> findByUserIdAndEnabledTrueAndDeletedAtIsNull(UUID userId);
    List<RecommendationRule> findByUserIdIsNullAndEnabledTrueAndDeletedAtIsNull();
    List<RecommendationRule> findByUserIdAndDeletedAtIsNullOrderByCategoryAscKeywordAsc(UUID userId);
    void deleteByUserIdAndDeletedAtIsNull(UUID userId);
}
