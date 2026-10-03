package com.referralcrm.service;

import com.referralcrm.domain.Resume;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ResumeRecommendationService {
    public record Result(Resume resume, String reason, List<String> matchingSkills, List<String> missingSkills) {
        public Result(Resume resume,String reason) { this(resume,reason,List.of(),List.of()); }
    }
    public Result recommend(List<Resume> resumes, String role) {
        return recommend(resumes, role, List.of(), Map.of());
    }
    public Result recommend(List<Resume> resumes, String role, List<String> requiredSkills, Map<UUID,List<String>> skillsByResume) {
        if (resumes.isEmpty()) return new Result(null, "Add a resume to get a resume recommendation.",List.of(),List.of());
        if(requiredSkills!=null&&!requiredSkills.isEmpty()) {
            List<String> required=requiredSkills.stream().filter(s->s!=null&&!s.isBlank()).map(String::trim).distinct().toList();
            Resume best=resumes.stream().max(Comparator.comparingInt((Resume r)->skillMatches(r,required,skillsByResume).size())
                .thenComparingInt(r->score(r,role==null?new String[0]:role.toLowerCase(Locale.ROOT).split("[^a-z0-9+#]+")))).orElseThrow();
            List<String> matching=skillMatches(best,required,skillsByResume);
            List<String> missing=required.stream().filter(s->!matching.stream().anyMatch(m->m.equalsIgnoreCase(s))).toList();
            String reason=matching.isEmpty()?"No saved resume skills match this job yet; review the resume before using it.":"Best skill coverage ("+matching.size()+" of "+required.size()+" required skills).";
            return new Result(best,reason,matching,missing);
        }
        String[] terms = role == null ? new String[0] : role.toLowerCase(Locale.ROOT).split("[^a-z0-9+#]+") ;
        Resume best = resumes.stream().max(Comparator.comparingInt(resume -> score(resume, terms))).orElseThrow();
        int score = score(best, terms);
        String reason = score > 0 ? "The resume label or filename matches the role keywords." : "This is your most recently added available resume; review it before using it.";
        return new Result(best, reason,List.of(),List.of());
    }
    private List<String> skillMatches(Resume resume,List<String> required,Map<UUID,List<String>> skillsByResume) {
        Set<String> saved=new LinkedHashSet<>();
        (skillsByResume.getOrDefault(resume.getId(),List.of())).forEach(s->saved.add(s.toLowerCase(Locale.ROOT)));
        return required.stream().filter(s->saved.contains(s.toLowerCase(Locale.ROOT))).toList();
    }
    private int score(Resume resume, String[] terms) {
        String text = (resume.getLabel() + " " + resume.getFileName()).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String term : terms) if (term.length() > 2 && text.contains(term)) score += 1;
        return score * 10000 + (int)Math.min(9999, resume.getCreatedAt() == null ? 0 : resume.getCreatedAt().getEpochSecond() % 10000);
    }
}
