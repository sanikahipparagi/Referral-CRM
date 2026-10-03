package com.referralcrm.service;

import com.referralcrm.domain.*;
import com.referralcrm.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {
    public record MetricGroup(String name, long count, long replies, BigDecimal responseRate) {}
    public record Analytics(BigDecimal responseRate, BigDecimal referralRate, BigDecimal interviewRate, BigDecimal offerRate, BigDecimal averageReplyHours,
                            List<MetricGroup> topCompanies, List<MetricGroup> topRoles, String mostSuccessfulResume, String bestPerformingMessage,
                            String mostResponsiveContactType) {}
    private final ContactRepository contacts; private final OutreachRepository outreach; private final CompanyRepository companies;
    private final ResumeRepository resumes; private final InterviewRepository interviews;
    public AnalyticsService(ContactRepository contacts, OutreachRepository outreach, CompanyRepository companies, ResumeRepository resumes, InterviewRepository interviews) {
        this.contacts=contacts; this.outreach=outreach; this.companies=companies; this.resumes=resumes; this.interviews=interviews;
    }

    public Analytics calculate(UUID userId) {
        List<Contact> people = contacts.findByUserIdAndDeletedAtIsNull(userId);
        List<Outreach> sent = outreach.findByUserIdAndDeletedAtIsNull(userId);
        List<Company> companyRows = companies.findByUserIdAndDeletedAtIsNull(userId);
        List<Resume> resumeRows = resumes.findByUserIdAndDeletedAtIsNull(userId);
        List<Interview> interviewRows = interviews.findByUserIdAndDeletedAtIsNull(userId);
        Map<UUID, Contact> personById = people.stream().collect(Collectors.toMap(Contact::getId, Function.identity()));
        Map<UUID, OffsetDateTime> firstSent = sent.stream().collect(Collectors.toMap(Outreach::getContactId, Outreach::getSentAt, (a,b) -> a.isBefore(b) ? a : b));
        List<Long> responseHours = people.stream().filter(c -> c.getRepliedAt() != null && firstSent.containsKey(c.getId()))
            .map(c -> Math.max(0, Duration.between(firstSent.get(c.getId()), c.getRepliedAt()).toHours())).toList();
        long contacted = firstSent.size(); long replied = people.stream().filter(c -> firstSent.containsKey(c.getId()) && responded(c)).count();
        long referred = people.stream().filter(c -> firstSent.containsKey(c.getId()) && c.getStatus() == ContactStatus.REFERRED).count();
        long interviewed = people.stream().filter(c -> firstSent.containsKey(c.getId()) && c.getStatus() == ContactStatus.INTERVIEW).count();
        long offers = Math.min(contacted,interviewRows.stream().filter(i -> "OFFER".equalsIgnoreCase(i.getResult())).count());
        Map<UUID, Company> companyById = companyRows.stream().collect(Collectors.toMap(Company::getId, Function.identity()));
        List<MetricGroup> companyStats = group(people, Contact::getCompanyId, id -> id == null ? "No company" : Optional.ofNullable(companyById.get(id)).map(Company::getName).orElse("Company"), repliedStatuses())
            .stream().sorted(Comparator.comparingLong(MetricGroup::replies).reversed().thenComparing(Comparator.comparingLong(MetricGroup::count).reversed())).limit(5).toList();
        List<MetricGroup> roleStats = group(people, c -> normalizeRole(c.getDesignation()), Function.identity(), repliedStatuses()).stream()
            .sorted(Comparator.comparing(MetricGroup::responseRate).reversed().thenComparing(Comparator.comparingLong(MetricGroup::count).reversed())).limit(5).toList();
        Map<UUID, Resume> resumeById = resumeRows.stream().collect(Collectors.toMap(Resume::getId, Function.identity()));
        String bestResume = sent.stream().filter(o -> o.getResumeId() != null && responded(personById.get(o.getContactId())))
            .collect(Collectors.groupingBy(Outreach::getResumeId, Collectors.counting())).entrySet().stream().max(Map.Entry.comparingByValue())
            .map(e -> Optional.ofNullable(resumeById.get(e.getKey())).map(Resume::getLabel).orElse(null)).orElse(null);
        String bestMessage = sent.stream().filter(o -> responded(personById.get(o.getContactId())))
            .collect(Collectors.groupingBy(Outreach::getMessageVersion, Collectors.counting())).entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        String responsiveType = people.stream().collect(Collectors.groupingBy(c -> contactType(c.getDesignation())))
            .entrySet().stream().map(e -> new MetricGroup(e.getKey(), e.getValue().size(), e.getValue().stream().filter(AnalyticsService::responded).count(), rate(e.getValue().stream().filter(AnalyticsService::responded).count(), e.getValue().size())))
            .filter(g -> g.count() > 0).max(Comparator.comparing(MetricGroup::responseRate).thenComparingLong(MetricGroup::count)).map(MetricGroup::name).orElse(null);
        BigDecimal avgHours = responseHours.isEmpty() ? BigDecimal.ZERO.setScale(1) : BigDecimal.valueOf(responseHours.stream().mapToLong(Long::longValue).average().orElse(0)).setScale(1, RoundingMode.HALF_UP);
        return new Analytics(rate(replied, contacted), rate(referred, contacted), rate(interviewed, contacted), rate(offers, contacted), avgHours,
            companyStats, roleStats, bestResume, bestMessage, responsiveType);
    }

    private Set<ContactStatus> repliedStatuses() { return EnumSet.of(ContactStatus.REPLIED, ContactStatus.REFERRED, ContactStatus.INTERVIEW); }
    private static boolean responded(Contact c) { return c != null && (c.getRepliedAt() != null || c.getStatus() == ContactStatus.REPLIED || c.getStatus() == ContactStatus.REFERRED || c.getStatus() == ContactStatus.INTERVIEW); }
    private <K> List<MetricGroup> group(List<Contact> people, Function<Contact,K> key, Function<K,String> label, Set<ContactStatus> success) {
        return people.stream().collect(Collectors.groupingBy(key)).entrySet().stream().map(e -> {
            long replies = e.getValue().stream().filter(c -> success.contains(c.getStatus()) || c.getRepliedAt() != null).count();
            return new MetricGroup(label.apply(e.getKey()), e.getValue().size(), replies, rate(replies, e.getValue().size()));
        }).toList();
    }
    private String normalizeRole(String role) { return role == null || role.isBlank() ? "Role not set" : role.trim(); }
    private String contactType(String designation) {
        String text = designation == null ? "" : designation.toLowerCase(Locale.ROOT);
        if (text.contains("recruit")) return "Recruiter";
        if (text.contains("manager") || text.contains("director") || text.contains("vp")) return "Engineering leadership";
        if (text.contains("engineer") || text.contains("developer")) return "Engineering";
        return "Other";
    }
    private static BigDecimal rate(long numerator, long denominator) { return denominator == 0 ? BigDecimal.ZERO.setScale(1) : BigDecimal.valueOf(numerator * 100.0 / denominator).setScale(1, RoundingMode.HALF_UP); }
}
