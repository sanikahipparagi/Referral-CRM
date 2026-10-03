package com.referralcrm.api;

import com.referralcrm.domain.ContactStatus;
import com.referralcrm.repository.*;
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
    private final ZoneId zone;

    public DashboardController(ContactRepository contacts, OutreachRepository outreach, InterviewRepository interviews,
                               CompanyRepository companies, @Value("${app.default-time-zone}") String timeZone) {
        this.contacts=contacts; this.outreach=outreach; this.interviews=interviews; this.companies=companies; this.zone=ZoneId.of(timeZone);
    }

    public record FollowUp(UUID id, String name, String designation, String companyName, OffsetDateTime lastSentAt) {}
    public record CompanySummary(UUID id, String name, long contactCount, long outreachCount, long referralCount) {}
    public record DashboardData(LocalDate date, long todayOutreach, long peopleContacted, long pendingFollowUps,
                                long replies, long referralsReceived, long interviews, BigDecimal responseRate,
                                BigDecimal referralRate, List<FollowUp> followUps, List<CompanySummary> companies) {}

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
        List<FollowUp> followUps=outreach.findNeedsFollowUp(userId, now.minusDays(7).toOffsetDateTime()).stream()
            .map(r->new FollowUp(r.getId(),r.getName(),r.getDesignation(),r.getCompanyName(),r.getLastSentAt())).toList();
        List<CompanySummary> companyStats=companies.dashboardStats(userId).stream()
            .map(c->new CompanySummary(c.getId(),c.getName(),c.getContactCount(),c.getOutreachCount(),c.getReferralCount())).toList();
        return new DashboardData(date,
            outreach.countByUserIdAndSentAtGreaterThanEqualAndSentAtLessThanAndDeletedAtIsNull(userId,dayStart,dayEnd),
            contacted,outreach.countNeedsFollowUp(userId,now.minusDays(7).toOffsetDateTime()),replies,referrals,interviews.countByUserIdAndDeletedAtIsNull(userId),
            rate(replies,contacted),rate(referrals,contacted),followUps,companyStats);
    }

    private BigDecimal rate(long numerator,long denominator) {
        return denominator==0?BigDecimal.ZERO.setScale(1):BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(denominator),1,RoundingMode.HALF_UP);
    }
}
