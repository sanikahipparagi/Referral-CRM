package com.referralcrm.api;

import com.referralcrm.domain.ContactStatus;
import com.referralcrm.repository.*;
import com.referralcrm.domain.JobStatus;
import com.referralcrm.service.OpportunityRankingService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final ContactRepository contacts;
    private final OutreachRepository outreach;
    private final InterviewRepository interviews;
    private final CompanyRepository companies;
    private final JobOpportunityRepository jobs;
    private final OpportunityRankingService ranking;
    private final ZoneId zone;
    private final int followUpDays;

    public DashboardController(ContactRepository contacts, OutreachRepository outreach, InterviewRepository interviews,
                               CompanyRepository companies, JobOpportunityRepository jobs, OpportunityRankingService ranking,
                               @Value("${app.default-time-zone}") String timeZone,
                               @Value("${app.follow-up-days:7}") int followUpDays) {
        this.contacts=contacts; this.outreach=outreach; this.interviews=interviews; this.companies=companies; this.jobs=jobs; this.ranking=ranking; this.zone=ZoneId.of(timeZone); this.followUpDays=Math.max(1,Math.min(followUpDays,365));
    }

    public record FollowUp(UUID id, String name, String designation, String companyName, OffsetDateTime lastSentAt) {}
    public record CompanySummary(UUID id, String name, long contactCount, long outreachCount, long referralCount) {}
    public record JobSummary(long jobsFound,long interestedJobs,long applications,long interviews,long offers,BigDecimal averageMatchScore,List<OpportunityRankingService.RankedOpportunity> topMatchingJobs) {}
    public record DashboardData(LocalDate date, int followUpDays, long todayOutreach, long peopleContacted, long pendingFollowUps,
                                long replies, long referralsReceived, long interviews, BigDecimal responseRate,
                                BigDecimal referralRate, List<FollowUp> followUps, List<CompanySummary> companies, JobSummary jobs) {}

    @GetMapping
    public DashboardData getDashboard(Authentication authentication) {
        UUID userId=(UUID)authentication.getPrincipal();
        ZonedDateTime now=ZonedDateTime.now(zone);
        LocalDate date=now.toLocalDate();
        OffsetDateTime dayStart=date.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime dayEnd=date.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        long contacted=outreach.countContactedPeople(userId);
        long replies=contacts.countByUserIdAndStatusAndDeletedAtIsNull(userId, ContactStatus.REPLIED);
        long referrals=contacts.countByUserIdAndStatusAndDeletedAtIsNull(userId, ContactStatus.REFERRED);
        List<FollowUp> followUps=outreach.findNeedsFollowUp(userId, now.minusDays(followUpDays).toOffsetDateTime()).stream()
            .map(r->new FollowUp(r.getId(),r.getName(),r.getDesignation(),r.getCompanyName(),r.getLastSentAt())).toList();
        List<CompanySummary> companyStats=companies.dashboardStats(userId).stream()
            .map(c->new CompanySummary(c.getId(),c.getName(),c.getContactCount(),c.getOutreachCount(),c.getReferralCount())).toList();
        List<OpportunityRankingService.RankedOpportunity> allRankedJobs=ranking.ranked(userId);
        List<OpportunityRankingService.RankedOpportunity> topJobs=allRankedJobs.stream().limit(5).toList();
        BigDecimal avgMatch=allRankedJobs.isEmpty()?BigDecimal.ZERO.setScale(1):BigDecimal.valueOf(allRankedJobs.stream().mapToInt(OpportunityRankingService.RankedOpportunity::matchScore).average().orElse(0)).setScale(1,RoundingMode.HALF_UP);
        JobSummary jobSummary=new JobSummary(count(userId,JobStatus.FOUND),count(userId,JobStatus.INTERESTED),count(userId,JobStatus.APPLIED),count(userId,JobStatus.INTERVIEW),count(userId,JobStatus.OFFER),avgMatch,topJobs);
        return new DashboardData(date,followUpDays,
            outreach.countByUserIdAndSentAtGreaterThanEqualAndSentAtLessThanAndDeletedAtIsNull(userId,dayStart,dayEnd),
            contacted,outreach.countNeedsFollowUp(userId,now.minusDays(followUpDays).toOffsetDateTime()),replies,referrals,interviews.countByUserIdAndDeletedAtIsNull(userId),
            rate(replies,contacted),rate(referrals,contacted),followUps,companyStats,jobSummary);
    }

    private long count(UUID userId,JobStatus status) { return jobs.countByUserIdAndStatusAndDeletedAtIsNull(userId,status); }

    private BigDecimal rate(long numerator,long denominator) {
        return denominator==0?BigDecimal.ZERO.setScale(1):BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(denominator),1,RoundingMode.HALF_UP);
    }
}
