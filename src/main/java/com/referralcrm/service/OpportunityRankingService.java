package com.referralcrm.service;

import com.referralcrm.domain.*;
import com.referralcrm.repository.AppUserRepository;
import com.referralcrm.repository.CompanyRepository;
import com.referralcrm.repository.JobOpportunityRepository;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** Weighted, explainable ranking: skills 40%, company priority 20%, role 20%, location 10%, experience 10%. */
@Service
public class OpportunityRankingService {
    public record RankedOpportunity(UUID id,String title,UUID companyId,String companyName,String location,JobStatus status,
        JobPriority priority,int matchScore,int rankingScore,List<String> reasons,UUID recommendedResumeId,String recommendedResume) {}
    private final JobOpportunityRepository jobs; private final CompanyRepository companies; private final AppUserRepository users; private final JobMatchingService matching;
    public OpportunityRankingService(JobOpportunityRepository jobs,CompanyRepository companies,AppUserRepository users,JobMatchingService matching) { this.jobs=jobs; this.companies=companies; this.users=users; this.matching=matching; }
    public List<RankedOpportunity> top(UUID userId,int limit) {
        return ranked(userId).stream().limit(limit).toList();
    }
    public List<RankedOpportunity> ranked(UUID userId) {
        String profile=users.findById(userId).map(AppUser::getProfileText).orElse("");
        return jobs.findByUserIdAndDeletedAtIsNull(userId).stream().filter(job->job.getStatus()!=JobStatus.REJECTED).map(job->rank(userId,job,profile)).sorted(Comparator.comparingInt(RankedOpportunity::rankingScore).reversed().thenComparing(RankedOpportunity::title,String.CASE_INSENSITIVE_ORDER)).toList();
    }
    private RankedOpportunity rank(UUID userId,JobOpportunity job,String profile) {
        JobMatchingService.MatchResult match=matching.calculate(userId,job);
        Company company=companies.findById(job.getCompanyId()).filter(c->c.getUserId().equals(userId)&&c.getDeletedAt()==null).orElse(null);
        int priority=company==null?0:company.isDreamCompany()?100:Math.max(0,Math.min(100,company.getPriority()*20));
        int role=similarity(job.getTitle(),profile);
        int location=job.getLocation()==null?0:(job.getLocation().toLowerCase(Locale.ROOT).contains("remote")||contains(profile,job.getLocation())?100:0);
        int experience=job.getExperienceLevel()==null?0:(contains(profile,job.getExperienceLevel())?100:0);
        int total=(int)Math.round(match.score()*0.40+priority*0.20+role*0.20+location*0.10+experience*0.10);
        List<String> reasons=new ArrayList<>(); if(!match.matchingSkills().isEmpty()) reasons.add("Skills: "+String.join(", ",match.matchingSkills()));
        if(company!=null&&(company.isDreamCompany()||company.getPriority()>=4)) reasons.add(company.isDreamCompany()?"Target company":"High company priority");
        if(role>0) reasons.add("Role aligns with your profile"); if(location==100) reasons.add("Location matches or is remote"); if(experience==100) reasons.add("Experience level aligns");
        return new RankedOpportunity(job.getId(),job.getTitle(),job.getCompanyId(),company==null?null:company.getName(),job.getLocation(),job.getStatus(),job.getPriority(),match.score(),total,List.copyOf(reasons),match.recommendedResumeId(),match.recommendedResume());
    }
    private int similarity(String title,String profile) {
        Set<String> titleTokens=tokens(title), profileTokens=tokens(profile); if(titleTokens.isEmpty()||profileTokens.isEmpty()) return 0;
        long common=titleTokens.stream().filter(profileTokens::contains).count(); return (int)Math.round((double)common/titleTokens.size()*100);
    }
    private Set<String> tokens(String text) { Set<String> result=new HashSet<>(); for(String token:Pattern.compile("[^a-z0-9+#]+").split(Objects.toString(text,"").toLowerCase(Locale.ROOT))) if(token.length()>2) result.add(token); return result; }
    private boolean contains(String text,String term) { return text!=null&&term!=null&&text.toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT)); }
}
