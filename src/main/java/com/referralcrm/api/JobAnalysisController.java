package com.referralcrm.api;

import com.referralcrm.domain.SuggestionStatus;
import com.referralcrm.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobs/{jobId}")
public class JobAnalysisController {
    public record SuggestionUpdate(@NotNull SuggestionStatus status) {}
    public record NotesUpdate(@Size(max=12000) String notes) {}
    private final ResumeAnalysisService analyses; private final ResumeImprovementService improvements; private final InterviewPreparationService preparation;
    public JobAnalysisController(ResumeAnalysisService analyses,ResumeImprovementService improvements,InterviewPreparationService preparation){this.analyses=analyses;this.improvements=improvements;this.preparation=preparation;}
    @PostMapping("/analysis") @ResponseStatus(HttpStatus.CREATED) public JobAnalysisService.JobAnalysisResult analyze(Authentication auth,@PathVariable UUID jobId){return analyses.analyze(uid(auth),jobId);}
    @GetMapping("/analysis") public List<JobAnalysisService.JobAnalysisResult> history(Authentication auth,@PathVariable UUID jobId){return analyses.history(uid(auth),jobId);}
    @GetMapping("/suggestions") public List<ResumeImprovementService.SuggestionView> suggestions(Authentication auth,@PathVariable UUID jobId,@RequestParam UUID resumeId){return improvements.list(uid(auth),jobId,resumeId);}
    @PatchMapping("/suggestions/{suggestionId}") public ResumeImprovementService.SuggestionView updateSuggestion(Authentication auth,@PathVariable UUID jobId,@PathVariable UUID suggestionId,@Valid @RequestBody SuggestionUpdate body){return improvements.updateStatus(uid(auth),jobId,suggestionId,body.status());}
    @PostMapping("/interview-prep") @ResponseStatus(HttpStatus.CREATED) public InterviewPreparationService.PreparationView generatePrep(Authentication auth,@PathVariable UUID jobId){return preparation.generate(uid(auth),jobId);}
    @GetMapping("/interview-prep") public List<InterviewPreparationService.PreparationView> prepHistory(Authentication auth,@PathVariable UUID jobId){return preparation.list(uid(auth),jobId);}
    @PutMapping("/interview-prep/{noteId}") public InterviewPreparationService.PreparationView updatePrep(Authentication auth,@PathVariable UUID jobId,@PathVariable UUID noteId,@Valid @RequestBody NotesUpdate body){return preparation.updateNotes(uid(auth),jobId,noteId,body.notes());}
    private UUID uid(Authentication auth){return (UUID)auth.getPrincipal();}
}
