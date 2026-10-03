package com.referralcrm.service;

import com.referralcrm.api.ApiException;
import com.referralcrm.domain.*;
import com.referralcrm.repository.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class OpportunityService {
    public record ContactOpportunity(UUID id, String name, String designation, UUID companyId, String companyName, String location, String linkedinUrl, String status, int recommendationScore, String reasoning, boolean needsFollowUp) {}
    public record CompanyOpportunity(UUID id, String name, short priority, boolean dreamCompany, String applicationStatus) {}
    public record Queue(List<ContactOpportunity> contacts, List<ContactOpportunity> suggestedContacts, List<CompanyOpportunity> suggestedCompanies) {}
    private final ContactRepository contacts;
    private final CompanyRepository companies;
    private final OutreachRepository outreach;
    private final RecommendationService recommendations;
    private final int followUpDays;

    public OpportunityService(ContactRepository contacts, CompanyRepository companies, OutreachRepository outreach, RecommendationService recommendations,
                              @Value("${app.follow-up-days:7}") int followUpDays) {
        this.contacts = contacts; this.companies = companies; this.outreach = outreach; this.recommendations = recommendations; this.followUpDays=Math.max(1,Math.min(followUpDays,365));
    }

    public Queue find(UUID userId, UUID companyId, String role, String status, String location, Integer priority) {
        ContactStatus contactStatus = null;
        if (status != null && !status.isBlank()) try { contactStatus = ContactStatus.valueOf(status.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown contact status"); }
        List<Company> ownedCompanies = companies.findByUserIdAndDeletedAtIsNull(userId);
        Map<UUID, Company> companyById = ownedCompanies.stream().collect(Collectors.toMap(Company::getId, x -> x));
        Specification<Contact> spec = (root, query, cb) -> cb.and(cb.equal(root.get("userId"), userId), cb.isNull(root.get("deletedAt")));
        if (companyId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("companyId"), companyId));
        if (contactStatus != null) { ContactStatus selected = contactStatus; spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), selected)); }
        if (role != null && !role.isBlank()) { String like = "%" + role.trim().toLowerCase(Locale.ROOT) + "%"; spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("designation")), like)); }
        if (location != null && !location.isBlank()) { String like = "%" + location.trim().toLowerCase(Locale.ROOT) + "%"; spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("location")), like)); }
        List<Contact> found = contacts.findAll(spec, PageRequest.of(0,500,Sort.by(Sort.Direction.DESC, "dateAdded"))).getContent();
        Set<UUID> followUps = outreach.findNeedsFollowUp(userId, OffsetDateTime.now().minusDays(followUpDays)).stream().map(OutreachRepository.FollowUpRecord::getId).collect(Collectors.toSet());
        List<ContactOpportunity> cards = found.stream().filter(c -> c.getCompanyId() == null || !companyById.containsKey(c.getCompanyId()) || priority == null || companyById.get(c.getCompanyId()).getPriority() == priority)
            .map(c -> { Company co = companyById.get(c.getCompanyId()); var recommendation = recommendations.recommend(userId, c, co, role); return new ContactOpportunity(c.getId(), c.getName(), c.getDesignation(), c.getCompanyId(), co == null ? null : co.getName(), c.getLocation(), c.getLinkedinUrl(), c.getStatus().name(), recommendation.score(), recommendation.reason(), followUps.contains(c.getId())); })
            .sorted(Comparator.comparingInt(ContactOpportunity::recommendationScore).reversed()).toList();
        Set<UUID> linkedCompanies = contacts.findByUserIdAndDeletedAtIsNull(userId).stream().map(Contact::getCompanyId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<CompanyOpportunity> suggestedCompanies = ownedCompanies.stream().filter(c -> !linkedCompanies.contains(c.getId()))
            .filter(c -> priority == null || c.getPriority() == priority)
            .sorted(Comparator.comparing(Company::isDreamCompany).reversed().thenComparing(Company::getPriority, Comparator.reverseOrder()).thenComparing(Company::getName))
            .limit(12).map(c -> new CompanyOpportunity(c.getId(), c.getName(), c.getPriority(), c.isDreamCompany(), c.getApplicationStatus())).toList();
        List<ContactOpportunity> suggestedContacts = cards.stream().filter(c -> c.status().equals("NOT_CONTACTED") || c.status().equals("MESSAGE_READY")).limit(8).toList();
        return new Queue(cards, suggestedContacts, suggestedCompanies);
    }
}
