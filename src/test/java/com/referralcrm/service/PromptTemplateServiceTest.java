package com.referralcrm.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.referralcrm.domain.PromptTemplate;
import com.referralcrm.repository.PromptTemplateRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PromptTemplateServiceTest {
    private final PromptTemplateRepository repository=mock(PromptTemplateRepository.class);
    private final PromptTemplateService service=new PromptTemplateService(repository);

    @Test void firstPersonalEditContinuesAfterTheBuiltInPromptVersion() {
        UUID userId=UUID.randomUUID(); PromptTemplate defaults=template(null,"EMAIL",1,"Default {contactName}");
        when(repository.findFirstByUserIdAndCategoryOrderByVersionDesc(userId,"EMAIL")).thenReturn(Optional.empty());
        when(repository.findFirstByUserIdIsNullAndCategoryAndDeletedAtIsNullOrderByVersionDesc("EMAIL")).thenReturn(Optional.of(defaults));
        when(repository.save(any(PromptTemplate.class))).thenAnswer(call->call.getArgument(0));
        PromptTemplate saved=service.saveVersion(userId,"EMAIL","Personal {contactName}");
        assertEquals(2,saved.getVersion()); assertTrue(saved.isActive()); assertEquals(userId,saved.getUserId());
    }

    @Test void renderingUsesDatabaseTextAndEscapedLineBreaks() {
        UUID userId=UUID.randomUUID(); when(repository.findFirstByUserIdAndCategoryAndActiveTrueAndDeletedAtIsNullOrderByVersionDesc(userId,"SHORT"))
            .thenReturn(Optional.of(template(userId,"SHORT",3,"Hi {contactName}\\nReady")));
        assertEquals("Hi Morgan\nReady",service.render(userId,"SHORT",java.util.Map.of("contactName","Morgan")));
    }

    private PromptTemplate template(UUID userId,String category,int version,String text) { PromptTemplate t=new PromptTemplate();t.setUserId(userId);t.setCategory(category);t.setVersion(version);t.setPromptText(text);t.setActive(true);return t; }
}
