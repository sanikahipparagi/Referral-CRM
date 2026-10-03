package com.referralcrm.service;

import com.referralcrm.api.ApiException;
import com.referralcrm.domain.*;
import com.referralcrm.repository.*;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Scores each owned resume against explicit job skills, parsed projects, and parsed experience. */
@Service
public class ResumeJobMatchService {
    public record MatchResult(int score,List<String> matchingSkills,List<String> missingSkills,int experienceMatch,
        List<String> strengths,List<String> weaknesses,List<String> recommendations,UUID recommendedResumeId,
        String recommendedResume,String resumeReason,String resumeText) {}
    private final ResumeRepository resumes; private final ResumeSkillRepository skills; private final ResumeDocumentRepository documents;
    private final ResumeProjectRepository projects; private final ResumeExperienceRepository experience; private final ResumeProfileService profile;
    public ResumeJobMatchService(ResumeRepository resumes,ResumeSkillRepository skills,ResumeDocumentRepository documents,
        ResumeProjectRepository projects,ResumeExperienceRepository experience,ResumeProfileService profile) {
        this.resumes=resumes;this.skills=skills;this.documents=documents;this.projects=projects;this.experience=experience;this.profile=profile;
    }
    public MatchResult match(UUID userId,JobOpportunity job) {
        List<Resume> available=resumes.findByUserIdAndDeletedAtIsNull(userId);
        if(available.isEmpty()) return new MatchResult(0,List.of(),extractRequired(job),50,List.of(),List.of("No resume has been uploaded yet."),List.of("Upload a PDF resume to compare your experience with this role."),null,null,"Add a resume to get a recommendation.","");
        List<UUID> ids=available.stream().map(Resume::getId).toList();
        Map<UUID,String> texts=new HashMap<>(); documents.findByResumeIdInAndDeletedAtIsNull(ids).forEach(d->texts.merge(d.getResumeId(),d.getExtractedText(),(a,b)->a.length()>=b.length()?a:b));
        Map<UUID,Set<String>> skillsByResume=new HashMap<>();
        skills.findByResumeIdInAndDeletedAtIsNull(ids).forEach(s->skillsByResume.computeIfAbsent(s.getResumeId(),x->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)).add(s.getSkill()));
        texts.forEach((id,text)->skillsByResume.computeIfAbsent(id,x->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)).addAll(profile.extractSkills(text)));
        Map<UUID,List<ResumeProject>> projectsByResume=new HashMap<>(); projects.findByResumeIdInAndDeletedAtIsNull(ids).forEach(p->projectsByResume.computeIfAbsent(p.getResumeId(),x->new ArrayList<>()).add(p));
        Map<UUID,List<ResumeExperience>> experienceByResume=new HashMap<>(); experience.findByResumeIdInAndDeletedAtIsNull(ids).forEach(e->experienceByResume.computeIfAbsent(e.getResumeId(),x->new ArrayList<>()).add(e));
        List<String> required=extractRequired(job);
        List<Candidate> candidates=available.stream().map(r->evaluate(r,required,texts.getOrDefault(r.getId(),""),skillsByResume.getOrDefault(r.getId(),Set.of()),projectsByResume.getOrDefault(r.getId(),List.of()),experienceByResume.getOrDefault(r.getId(),List.of()),job)).toList();
        Candidate best=candidates.stream().max(Comparator.comparingInt(Candidate::score)).orElseThrow();
        return new MatchResult(best.score,best.matching,best.missing,best.experienceMatch,best.strengths,best.weaknesses,best.recommendations,best.resume.getId(),best.resume.getLabel(),best.reason,best.text);
    }
    private Candidate evaluate(Resume resume,List<String> required,String text,Set<String> savedSkills,List<ResumeProject> projects,List<ResumeExperience> experience,JobOpportunity job) {
        Set<String> projectTextSkills=new TreeSet<>(String.CASE_INSENSITIVE_ORDER); String projectText=projects.stream().map(p->p.getName()+" "+p.getDescription()+" "+String.join(" ",p.getTechnologies())).reduce("",(a,b)->a+" "+b);
        projects.forEach(p->projectTextSkills.addAll(p.getTechnologies())); projectTextSkills.addAll(profile.extractSkills(projectText));
        Set<String> evidenced=new TreeSet<>(String.CASE_INSENSITIVE_ORDER); evidenced.addAll(savedSkills); evidenced.addAll(projectTextSkills); evidenced.addAll(profile.extractSkills(text));
        List<String> matching=required.stream().filter(s->evidenced.contains(s)||contains(text,s)||contains(projectText,s)).distinct().toList();
        List<String> missing=required.stream().filter(s->matching.stream().noneMatch(m->m.equalsIgnoreCase(s))).toList();
        int skillScore=required.isEmpty()?50:(int)Math.round((double)matching.size()*100/required.size());
        List<ResumeExperience> expRows=experience;
        int expMatch=experienceMatch(job.getExperienceLevel(),expRows,text);
        int projectScore=required.isEmpty()?50:(int)Math.round((double)required.stream().filter(projectTextSkills::contains).count()*100/required.size());
        int score=(int)Math.round(skillScore*.65+projectScore*.20+expMatch*.15);
        List<String> strengths=new ArrayList<>(); if(!matching.isEmpty())strengths.add("Resume evidence matches "+String.join(", ",matching)+".");
        projects.stream().filter(p->p.getTechnologies().stream().anyMatch(t->matching.stream().anyMatch(m->m.equalsIgnoreCase(t)))).limit(2).forEach(p->strengths.add("Relevant project: "+p.getName()+"."));
        List<String> weaknesses=new ArrayList<>(); if(!missing.isEmpty())weaknesses.add("Not found in the saved resume: "+String.join(", ",missing)+"."); if(expRows.isEmpty()&&job.getExperienceLevel()!=null&&!job.getExperienceLevel().isBlank())weaknesses.add("Experience section could not be confidently structured from the PDF.");
        List<String> recommendations=new ArrayList<>(); if(!matching.isEmpty())recommendations.add("Bring verified experience with "+String.join(", ",matching)+" closer to the top of the resume.");
        if(!missing.isEmpty())recommendations.add("Only if accurate, add evidence for "+String.join(", ",missing)+" in a relevant project or experience bullet.");
        if(!projects.isEmpty())recommendations.add("Highlight the most relevant project and include verified outcomes or scale.");
        String reason=matching.isEmpty()?"No required skills were found in this resume's extracted text or structured skills.":"Best available coverage: "+matching.size()+" of "+required.size()+" listed skills, including project and experience evidence.";
        return new Candidate(resume,score,matching,missing,expMatch,strengths,weaknesses,recommendations,reason,text);
    }
    private int experienceMatch(String required,List<ResumeExperience> entries,String text) {
        if(required==null||required.isBlank())return 50; if(entries.isEmpty())return contains(text,required)?70:20;
        String all=entries.stream().map(e->e.getRole()+" "+e.getDuration()+" "+e.getDescription()).reduce("",(a,b)->a+" "+b);
        return contains(all,required)?100:65;
    }
    public List<String> extractRequired(JobOpportunity job) {
        if(job.getSkills()!=null&&!job.getSkills().isEmpty())return job.getSkills().stream().map(String::strip).filter(s->!s.isBlank()).distinct().toList();
        return profile.extractSkills(job.getDescription());
    }
    private boolean contains(String text,String phrase) { return text!=null&&phrase!=null&&Pattern.compile("(?i)(?<![a-z0-9])"+Pattern.quote(phrase)+"(?![a-z0-9])").matcher(text).find(); }
    private record Candidate(Resume resume,int score,List<String> matching,List<String> missing,int experienceMatch,List<String> strengths,List<String> weaknesses,List<String> recommendations,String reason,String text) {}
}
