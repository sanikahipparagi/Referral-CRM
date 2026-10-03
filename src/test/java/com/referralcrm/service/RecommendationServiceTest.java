package com.referralcrm.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.referralcrm.domain.Contact;
import com.referralcrm.domain.Company;
import com.referralcrm.domain.RecommendationRule;
import com.referralcrm.repository.RecommendationRuleRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecommendationServiceTest {
    private final RecommendationRuleRepository repository = mock(RecommendationRuleRepository.class);
    private final RecommendationService service = new RecommendationService(repository);

    @Test void scoreUsesEditableRulesAndExplainsTheMatchedTerms() {
        UUID userId=UUID.randomUUID(); Contact contact=new Contact(); contact.setDesignation("Senior Backend Engineer");
        Company company=new Company(); company.setName("Acme"); company.setPriority((short)4);
        RecommendationRule java=rule("java",10); RecommendationRule backend=rule("backend",8);
        when(repository.findByUserIdAndDeletedAtIsNullOrderByCategoryAscKeywordAsc(userId)).thenReturn(List.of(java,backend));

        RecommendationService.Recommendation result=service.recommend(userId,contact,company,"Java backend engineer");

        assertEquals(68,result.score());
        assertTrue(result.reason().contains("java")); assertTrue(result.reason().contains("backend"));
        assertTrue(result.reason().contains("Likely referral contact"));
        verify(repository,never()).findByUserIdIsNullAndEnabledTrueAndDeletedAtIsNull();
    }

    @Test void scoreIsClampedAtOneHundred() {
        UUID userId=UUID.randomUUID(); Contact contact=new Contact(); contact.setNotes("Java Spring AWS backend recruiter mentor referral");
        when(repository.findByUserIdAndDeletedAtIsNullOrderByCategoryAscKeywordAsc(userId)).thenReturn(List.of(rule("java",40),rule("spring",40),rule("aws",40),rule("backend",40),rule("recruiter",40)));
        assertEquals(100,service.recommend(userId,contact,null,null).score());
    }

    @Test void aUsersDisabledRulesOverrideTheSystemDefaults() {
        UUID userId=UUID.randomUUID(); Contact contact=new Contact(); contact.setNotes("Java backend");
        RecommendationRule disabled=rule("java",40); disabled.setEnabled(false);
        when(repository.findByUserIdAndDeletedAtIsNullOrderByCategoryAscKeywordAsc(userId)).thenReturn(List.of(disabled));
        assertEquals(38,service.recommend(userId,contact,null,null).score());
        verify(repository,never()).findByUserIdIsNullAndEnabledTrueAndDeletedAtIsNull();
    }

    private RecommendationRule rule(String keyword,int weight) { RecommendationRule r=new RecommendationRule(); r.setKeyword(keyword); r.setWeight((short)weight); r.setEnabled(true); return r; }
}
