package com.referralcrm.service;

import com.referralcrm.api.ApiException;
import com.referralcrm.domain.*;
import com.referralcrm.repository.*;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Deterministic skill/profile matcher. The API can later delegate to an LLMProvider without changing callers. */
@Service
public class JobMatchingService {
    public record MatchResult(int score,List<String> matchingSkills,List<String> missingSkills,String recommendation,
                              UUID recommendedResumeId,String recommendedResume,String resumeReason) {}
    private static final Pattern TOKEN=Pattern.compile("[^a-z0-9+#.]+");
    private final JobOpportunityRepository jobs;
    private final AppUserRepository users;
    private final ResumeRepository resumes;
    private final ResumeSkillRepository resumeSkills;
    private final ResumeRecommendationService recommendations;
    public JobMatchingService(JobOpportunityRepository jobs, AppUserRepository users, ResumeRepository resumes,
                              ResumeSkillRepository resumeSkills, ResumeRecommendationService recommendations) {
        this.jobs=jobs; this.users=users; this.resumes=resumes; this.resumeSkills=resumeSkills; this.recommendations=recommendations;
    }
    @Transactional
    public MatchResult match(UUID userId, JobOpportunity job) {
        MatchResult result=calculate(userId,job);
        job.setMatchScore((short)result.score()); jobs.save(job);
        return result;
    }
    public MatchResult calculate(UUID userId, JobOpportunity job) {
        if(!job.getUserId().equals(userId)||job.getDeletedAt()!=null) throw new ApiException(HttpStatus.NOT_FOUND,"Job not found");
        String profile=users.findById(userId).map(AppUser::getProfileText).orElse("");
        List<Resume> available=resumes.findByUserIdAndDeletedAtIsNull(userId);
        Map<UUID,List<String>> skillsByResume=new HashMap<>();
        if(!available.isEmpty()) resumeSkills.findByResumeIdInAndDeletedAtIsNull(available.stream().map(Resume::getId).toList())
            .forEach(s->skillsByResume.computeIfAbsent(s.getResumeId(),ignored->new ArrayList<>()).add(s.getSkill()));
        List<String> required=new ArrayList<>(job.getSkills()==null?Set.of():job.getSkills());
        if(required.isEmpty()) {
            Set<String> known=new TreeSet<>(String.CASE_INSENSITIVE_ORDER); skillsByResume.values().forEach(known::addAll);
            String description=job.getDescription().toLowerCase(Locale.ROOT);
            required=known.stream().filter(skill->containsTerm(description,skill)).toList();
        }
        List<String> profileTokens=Arrays.stream(TOKEN.split(Objects.toString(profile,"").toLowerCase(Locale.ROOT))).filter(s->s.length()>1).toList();
        List<String> matching=required.stream().filter(skill->containsTerm(profile,skill)||skillsByResume.values().stream().flatMap(Collection::stream).anyMatch(s->s.equalsIgnoreCase(skill))).distinct().toList();
        List<String> missing=required.stream().filter(s->matching.stream().noneMatch(m->m.equalsIgnoreCase(s))).toList();
        int skillScore=required.isEmpty()?(profile.isBlank()?0:25):(int)Math.round((double)matching.size()*100/required.size());
        int profileOverlap=(int)Arrays.stream(TOKEN.split(job.getDescription().toLowerCase(Locale.ROOT))).filter(t->t.length()>3&&profileTokens.contains(t)).distinct().count();
        int score=required.isEmpty()?Math.min(100,profileOverlap*5):Math.min(100,(int)Math.round(skillScore*0.8+Math.min(100,profileOverlap*5)*0.2));
        ResumeRecommendationService.Result resumeResult=recommendations.recommend(available,job.getTitle(),required,skillsByResume);
        String recommendation=score>=75?"Strong match — prioritize this opportunity.":score>=50?"Good potential — review the missing skills and role details.":"Consider this opportunity after comparing requirements with your profile.";
        return new MatchResult(score,matching,missing,recommendation,resumeResult.resume()==null?null:resumeResult.resume().getId(),resumeResult.resume()==null?null:resumeResult.resume().getLabel(),resumeResult.reason());
    }
    private boolean containsTerm(String haystack,String term) { return haystack!=null&&Pattern.compile("(?i)(?<![a-z0-9])"+Pattern.quote(term)+"(?![a-z0-9])").matcher(haystack).find(); }
}
