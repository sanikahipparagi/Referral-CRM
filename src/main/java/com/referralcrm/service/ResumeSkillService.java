package com.referralcrm.service;

import com.referralcrm.api.ApiException;
import com.referralcrm.domain.Resume;
import com.referralcrm.domain.ResumeSkill;
import com.referralcrm.repository.ResumeRepository;
import com.referralcrm.repository.ResumeSkillRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumeSkillService {
    public record SkillView(UUID id, UUID resumeId, String skill) {}
    private final ResumeRepository resumes;
    private final ResumeSkillRepository skills;
    public ResumeSkillService(ResumeRepository resumes, ResumeSkillRepository skills) { this.resumes=resumes; this.skills=skills; }
    public List<SkillView> list(UUID userId, UUID resumeId) { owned(userId,resumeId); return skills.findByResumeIdAndDeletedAtIsNullOrderBySkillAsc(resumeId).stream().map(this::view).toList(); }
    @Transactional public List<SkillView> replace(UUID userId, UUID resumeId, List<String> requested) {
        owned(userId,resumeId);
        List<ResumeSkill> active=skills.findByResumeIdAndDeletedAtIsNullOrderBySkillAsc(resumeId);
        active.forEach(s->s.setDeletedAt(OffsetDateTime.now())); skills.saveAll(active);
        if(requested==null) return List.of();
        List<String> normalized=requested.stream().filter(s->s!=null&&!s.isBlank()).map(String::trim).map(s->s.length()>100?s.substring(0,100):s)
            .filter(s->!s.isBlank()).collect(java.util.stream.Collectors.toMap(s->s.toLowerCase(Locale.ROOT),s->s,(a,b)->a,java.util.LinkedHashMap::new)).values().stream().toList();
        return skills.saveAll(normalized.stream().map(skill->{ ResumeSkill row=new ResumeSkill(); row.setResumeId(resumeId); row.setSkill(skill); return row; }).toList()).stream().map(this::view).toList();
    }
    private Resume owned(UUID userId,UUID id) { return resumes.findById(id).filter(r->r.getUserId().equals(userId)&&r.getDeletedAt()==null).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Resume not found")); }
    private SkillView view(ResumeSkill s) { return new SkillView(s.getId(),s.getResumeId(),s.getSkill()); }
}
