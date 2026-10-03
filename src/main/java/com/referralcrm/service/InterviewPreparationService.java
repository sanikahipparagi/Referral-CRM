package com.referralcrm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.referralcrm.api.ApiException;
import com.referralcrm.domain.InterviewPreparationNote;
import com.referralcrm.domain.JobOpportunity;
import com.referralcrm.repository.InterviewPreparationRepository;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InterviewPreparationService {
    public record PreparationView(UUID id,UUID jobId,UUID resumeId,List<String> technicalTopics,List<String> systemDesignTopics,List<String> possibleQuestions,String notes) {}
    private final JobService jobs; private final ResumeJobMatchService matching; private final InterviewPreparationRepository notes; private final ObjectMapper json;
    public InterviewPreparationService(JobService jobs,ResumeJobMatchService matching,InterviewPreparationRepository notes,ObjectMapper json){this.jobs=jobs;this.matching=matching;this.notes=notes;this.json=json;}
    @Transactional public PreparationView generate(UUID userId,UUID jobId) {
        JobOpportunity job=jobs.get(userId,jobId); ResumeJobMatchService.MatchResult match=matching.match(userId,job);
        List<String> technical=matching.extractRequired(job).stream().limit(12).toList();
        List<String> design=List.of("Scalability and service boundaries","Data consistency and failure handling","Observability and operational readiness");
        if(contains(job.getDescription(),"payment"))design=new ArrayList<>(List.of("Payment authorization and ledger boundaries","Idempotency, retries, and duplicate transaction prevention","Reconciliation and auditability","Failure recovery across distributed payment services"));
        if(contains(job.getDescription(),"event")||contains(job.getDescription(),"kafka")){List<String> enriched=new ArrayList<>(design);enriched.add("Event-driven processing, ordering, and delivery guarantees");design=List.copyOf(enriched);}
        List<String> questions=new ArrayList<>();
        if(!technical.isEmpty()) {String key=technical.getFirst();questions.add("Describe a production problem where you used "+key+". What trade-offs did you make?");questions.add("How would you test and operate a service built around "+key+"?");}
        questions.add("Walk through a system you designed: requirements, architecture, bottlenecks, and trade-offs.");
        questions.add("Tell me about a measurable outcome from a project and how you verified the result.");
        questions.add("Which requirement in this role would you want to clarify before proposing a design?");
        InterviewPreparationNote note=new InterviewPreparationNote();note.setJobId(jobId);note.setResumeId(match.recommendedResumeId());note.setTechnicalTopics(write(technical));note.setSystemDesignTopics(write(design));note.setPossibleQuestions(write(questions));note.setNotes("");
        return view(notes.save(note));
    }
    @Transactional(readOnly=true) public List<PreparationView> list(UUID userId,UUID jobId) { jobs.get(userId,jobId);return notes.findByJobIdAndDeletedAtIsNullOrderByCreatedAtDesc(jobId).stream().map(this::view).toList(); }
    @Transactional public PreparationView updateNotes(UUID userId,UUID jobId,UUID noteId,String text) {
        jobs.get(userId,jobId);InterviewPreparationNote note=notes.findByIdAndJobIdAndDeletedAtIsNull(noteId,jobId).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Preparation note not found"));note.setNotes(text==null?"":text.trim());return view(notes.save(note));
    }
    private PreparationView view(InterviewPreparationNote note){return new PreparationView(note.getId(),note.getJobId(),note.getResumeId(),read(note.getTechnicalTopics()),read(note.getSystemDesignTopics()),read(note.getPossibleQuestions()),note.getNotes());}
    private String write(List<String> values){try{return json.writeValueAsString(values);}catch(JsonProcessingException e){throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,"Preparation could not be saved");}}
    private List<String> read(String value){try{return json.readValue(value,new TypeReference<>(){});}catch(JsonProcessingException e){throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,"Preparation could not be read");}}
    private boolean contains(String text,String term){return text!=null&&text.toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT));}
}
