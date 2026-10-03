package com.referralcrm.api;

import com.referralcrm.service.ResumeSkillService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/resumes/{resumeId}/skills")
public class ResumeSkillController {
    public record SkillsRequest(@Size(max=100) List<@NotBlank @Size(max=100) String> skills) {}
    private final ResumeSkillService skills;
    public ResumeSkillController(ResumeSkillService skills) { this.skills=skills; }
    @GetMapping public List<ResumeSkillService.SkillView> list(Authentication auth,@PathVariable UUID resumeId) { return skills.list((UUID)auth.getPrincipal(),resumeId); }
    @PutMapping public List<ResumeSkillService.SkillView> replace(Authentication auth,@PathVariable UUID resumeId,@Valid @RequestBody SkillsRequest request) { return skills.replace((UUID)auth.getPrincipal(),resumeId,request.skills()); }
}
