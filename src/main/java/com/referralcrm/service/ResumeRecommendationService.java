package com.referralcrm.service;

import com.referralcrm.domain.Resume;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ResumeRecommendationService {
    public record Result(Resume resume, String reason) {}
    public Result recommend(List<Resume> resumes, String role) {
        if (resumes.isEmpty()) return new Result(null, "Add a resume to get a resume recommendation.");
        String[] terms = role == null ? new String[0] : role.toLowerCase(Locale.ROOT).split("[^a-z0-9+#]+") ;
        Resume best = resumes.stream().max(Comparator.comparingInt(resume -> score(resume, terms))).orElseThrow();
        int score = score(best, terms);
        String reason = score > 0 ? "The resume label or filename matches the role keywords." : "This is your most recently added available resume; review it before using it.";
        return new Result(best, reason);
    }
    private int score(Resume resume, String[] terms) {
        String text = (resume.getLabel() + " " + resume.getFileName()).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String term : terms) if (term.length() > 2 && text.contains(term)) score += 1;
        return score * 10000 + (int)Math.min(9999, resume.getCreatedAt() == null ? 0 : resume.getCreatedAt().getEpochSecond() % 10000);
    }
}
