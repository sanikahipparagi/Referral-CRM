package com.referralcrm.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.referralcrm.domain.Resume;
import com.referralcrm.domain.ResumeExperience;
import com.referralcrm.domain.ResumeProject;
import com.referralcrm.repository.ResumeExperienceRepository;
import com.referralcrm.repository.ResumeProjectRepository;
import com.referralcrm.repository.ResumeRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ResumeProfileServiceTest {
    @Test void extractsCatalogSkillsAndParsesProjectAndExperienceSections() {
        UUID userId=UUID.randomUUID(),resumeId=UUID.randomUUID(); Resume resume=new Resume(); resume.setId(resumeId); resume.setUserId(userId); resume.setLabel("Backend");
        ResumeRepository resumes=mock(ResumeRepository.class); ResumeSkillService skills=mock(ResumeSkillService.class);
        ResumeProjectRepository projects=mock(ResumeProjectRepository.class); ResumeExperienceRepository experience=mock(ResumeExperienceRepository.class);
        when(resumes.findById(resumeId)).thenReturn(Optional.of(resume));
        when(projects.findByResumeIdAndDeletedAtIsNullOrderByCreatedAtDesc(resumeId)).thenReturn(List.of());
        when(experience.findByResumeIdAndDeletedAtIsNullOrderByCreatedAtDesc(resumeId)).thenReturn(List.of());
        when(projects.saveAll(any())).thenAnswer(call -> call.getArgument(0));
        when(experience.saveAll(any())).thenAnswer(call -> call.getArgument(0));
        ResumeProfileService service=new ResumeProfileService(resumes,skills,projects,experience);
        String text="Skills\nJava Spring Boot AWS\n\nExperience\nSoftware Engineer | Acme\n2021 - Present\nBuilt payment APIs with Kafka\n\nProjects\nPayments Platform\nCreated a payment processing service using Java, Spring Boot and Kafka";

        service.extractAndSave(userId,resumeId,text);

        verify(skills).replace(userId,resumeId,List.of("Java","Spring","Spring Boot","Kafka","AWS","Payments","Payment Processing"));
        @SuppressWarnings("unchecked") ArgumentCaptor<Iterable<ResumeProject>> projectCaptor=ArgumentCaptor.forClass(Iterable.class);
        verify(projects,times(2)).saveAll(projectCaptor.capture());
        List<ResumeProject> parsedProjects=toList(projectCaptor.getAllValues().getLast());
        assertEquals(1,parsedProjects.size());
        assertEquals("Payments Platform",parsedProjects.getFirst().getName());
        assertTrue(parsedProjects.getFirst().getTechnologies().containsAll(List.of("Java","Spring Boot","Kafka","Payment Processing")));

        @SuppressWarnings("unchecked") ArgumentCaptor<Iterable<ResumeExperience>> experienceCaptor=ArgumentCaptor.forClass(Iterable.class);
        verify(experience,times(2)).saveAll(experienceCaptor.capture());
        List<ResumeExperience> parsedExperience=toList(experienceCaptor.getAllValues().getLast());
        assertEquals(1,parsedExperience.size());
        assertEquals("Software Engineer",parsedExperience.getFirst().getRole());
        assertEquals("Acme",parsedExperience.getFirst().getCompany());
        assertEquals("2021 - Present",parsedExperience.getFirst().getDuration());
        assertTrue(parsedExperience.getFirst().getDescription().contains("Built payment APIs with Kafka"));
    }

    private <T> List<T> toList(Iterable<T> values) {
        java.util.ArrayList<T> result=new java.util.ArrayList<>(); values.forEach(result::add); return result;
    }
}
