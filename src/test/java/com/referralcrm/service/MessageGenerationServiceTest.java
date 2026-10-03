package com.referralcrm.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.referralcrm.domain.AppUser;
import com.referralcrm.domain.Contact;
import com.referralcrm.domain.GeneratedMessage;
import com.referralcrm.repository.GeneratedMessageRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class MessageGenerationServiceTest {
    private final GeneratedMessageRepository repository=mock(GeneratedMessageRepository.class);
    private final PromptTemplateService prompts=mock(PromptTemplateService.class);
    private final MessageGenerationService service=new MessageGenerationService(repository,prompts);

    @Test void savesEditableDraftVariantsWithoutMarkingAnyAsSent() {
        UUID userId=UUID.randomUUID(); AppUser user=new AppUser(); user.setFullName("Alex");
        Contact contact=new Contact(); contact.setId(UUID.randomUUID()); contact.setName("Jordan"); contact.setDesignation("Staff Engineer");
        when(prompts.render(eq(userId),anyString(),anyMap())).thenAnswer(call->"Prepared " + call.getArgument(1));
        when(repository.findFirstByContactIdAndVariantAndDeletedAtIsNullOrderByVersionDesc(any(),anyString())).thenReturn(Optional.empty());
        when(repository.save(any(GeneratedMessage.class))).thenAnswer(call->call.getArgument(0));

        List<GeneratedMessage> drafts=service.generate(userId,user,contact,null,null,"Backend Engineer",new RecommendationService.Recommendation(80,"Matched Java"),"No resume available",false);

        assertEquals(4,drafts.size()); assertTrue(drafts.stream().allMatch(m->"DRAFT".equals(m.getStatus())));
        assertTrue(drafts.stream().allMatch(m->m.getSentAt()==null)); assertEquals(4,drafts.stream().map(GeneratedMessage::getVariant).distinct().count());
        verify(repository,times(4)).save(any(GeneratedMessage.class));
    }

    @Test void followUpModeOnlyCreatesFollowUpVariant() {
        UUID userId=UUID.randomUUID(); AppUser user=new AppUser(); user.setFullName("Alex");
        Contact contact=new Contact(); contact.setId(UUID.randomUUID()); contact.setName("Jordan");
        when(prompts.render(eq(userId),anyString(),anyMap())).thenReturn("Polite follow-up");
        when(repository.findFirstByContactIdAndVariantAndDeletedAtIsNullOrderByVersionDesc(any(),anyString())).thenReturn(Optional.empty());
        when(repository.save(ArgumentMatchers.<GeneratedMessage>any())).thenAnswer(call->call.getArgument(0));

        List<GeneratedMessage> drafts=service.generate(userId,user,contact,null,null,"Java Engineer",new RecommendationService.Recommendation(50,"Role match"),"Resume available",true);

        assertEquals(1,drafts.size()); assertEquals("FOLLOW_UP",drafts.getFirst().getVariant()); assertEquals("DRAFT",drafts.getFirst().getStatus());
    }
}
