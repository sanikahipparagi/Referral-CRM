package com.referralcrm.service;

import static org.junit.jupiter.api.Assertions.*;

import com.referralcrm.domain.Resume;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResumeRecommendationServiceTest {
    private final ResumeRecommendationService service = new ResumeRecommendationService();

    @Test void recommendsResumeWhoseLabelMatchesTheRole() {
        Resume fullStack=resume("Full Stack","full-stack.pdf"); Resume java=resume("Java Backend","java-backend.pdf");
        var result=service.recommend(List.of(fullStack,java),"Java backend engineer");
        assertSame(java,result.resume()); assertTrue(result.reason().contains("matches"));
    }

    @Test void explainsWhenThereIsNoResumeToRecommend() {
        var result=service.recommend(List.of(),"Backend Engineer");
        assertNull(result.resume()); assertTrue(result.reason().contains("Add a resume"));
    }

    @Test void prefersResumeWithTheBestRequiredSkillCoverage() {
        Resume generic=resume("Full Stack","full.pdf"); generic.setId(UUID.randomUUID());
        Resume backend=resume("Java Backend","backend.pdf"); backend.setId(UUID.randomUUID());
        var result=service.recommend(List.of(generic,backend),"Backend Engineer",List.of("Java","Spring Boot","AWS"),Map.of(
            generic.getId(),List.of("Java"),backend.getId(),List.of("Java","Spring Boot","AWS")));
        assertSame(backend,result.resume()); assertEquals(List.of("Java","Spring Boot","AWS"),result.matchingSkills()); assertTrue(result.missingSkills().isEmpty());
    }

    private Resume resume(String label,String fileName) { Resume r=new Resume(); r.setLabel(label); r.setFileName(fileName); return r; }
}
