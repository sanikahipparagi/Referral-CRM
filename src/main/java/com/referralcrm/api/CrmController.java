package com.referralcrm.api;

import com.referralcrm.domain.*;
import com.referralcrm.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CrmController {
    private final CompanyRepository companies; private final ContactRepository contacts; private final ResumeRepository resumes;
    private final OutreachRepository outreach; private final InterviewRepository interviews;
    public CrmController(CompanyRepository companies, ContactRepository contacts, ResumeRepository resumes, OutreachRepository outreach, InterviewRepository interviews) {
        this.companies=companies; this.contacts=contacts; this.resumes=resumes; this.outreach=outreach; this.interviews=interviews;
    }
    private UUID uid(Authentication a) { return (UUID)a.getPrincipal(); }
    private <T extends AuditedEntity> T live(Optional<T> value) { return value.filter(x->x.getDeletedAt()==null).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Record not found")); }
    private <T extends AuditedEntity> T owned(Optional<T> value, UUID user) { T x=live(value); if(!owner(x).equals(user)) throw new ApiException(HttpStatus.NOT_FOUND,"Record not found"); return x; }
    private UUID owner(AuditedEntity x) {
        if(x instanceof Company y) return y.getUserId(); if(x instanceof Contact y) return y.getUserId(); if(x instanceof Resume y) return y.getUserId();
        if(x instanceof Outreach y) return y.getUserId(); if(x instanceof Interview y) return y.getUserId(); throw new IllegalArgumentException();
    }
    private <T extends AuditedEntity> Page<T> page(JpaSpecificationExecutor<T> repo, UUID user, String search, Pageable p) {
        Specification<T> spec=(root,q,cb)->cb.and(cb.equal(root.get("userId"),user),cb.isNull(root.get("deletedAt")));
        if(search!=null&&!search.isBlank()) {
            String like="%"+search.toLowerCase(Locale.ROOT)+"%";
            spec=spec.and((root,q,cb)->cb.or(cb.like(cb.lower(root.get("name").as(String.class)),like), cb.like(cb.lower(root.get("notes").as(String.class)),like)));
        }
        return repo.findAll(spec,p);
    }
    private Pageable pageable(int page,int size,String sort,String direction) {
        if(size<1||size>100||page<0) throw new ApiException(HttpStatus.BAD_REQUEST,"page must be >= 0 and size must be between 1 and 100");
        Sort s=Sort.by("DESC".equalsIgnoreCase(direction)?Sort.Direction.DESC:Sort.Direction.ASC,sort);
        return PageRequest.of(page,size,s);
    }
    public record CompanyDto(@NotBlank @Size(max=200) String name, @Size(max=2048) String careerPage, @Min(1) @Max(5) short priority, boolean dreamCompany, @Size(max=32) String applicationStatus, String notes) {}
    public record ContactDto(@NotBlank @Size(max=160) String name, UUID companyId, @Size(max=2048) String linkedinUrl, @Size(max=200) String designation, @Size(max=200) String location, @Email @Size(max=320) String email, @Size(max=120) String source, LocalDate dateAdded, String notes, ContactStatus status) {}
    public record ResumeDto(@NotBlank @Size(max=120) String label, @NotBlank String fileName, @NotBlank @Size(max=1024) String storageKey, @NotBlank @Size(max=120) String contentType, @Positive long fileSize) {}
    public record OutreachDto(@NotNull UUID contactId, UUID resumeId, @NotBlank String channel, @NotBlank String messageVersion, @NotBlank String messageText, @NotNull OffsetDateTime sentAt) {}
    public record InterviewDto(@NotNull UUID companyId, @NotBlank @Size(max=160) String round, OffsetDateTime interviewAt, @NotBlank @Size(max=32) String result, String feedback, @DecimalMin("0.0") BigDecimal packageAmount, String notes) {}

    @PostMapping("/companies") @ResponseStatus(HttpStatus.CREATED) public Company createCompany(Authentication a,@Valid @RequestBody CompanyDto d) { Company x=new Company(); x.setUserId(uid(a)); companyFields(x,d); return companies.save(x); }
    @GetMapping("/companies") public Page<Company> listCompanies(Authentication a,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="name") String sort,@RequestParam(defaultValue="asc") String direction,@RequestParam(required=false) String search) { return page(companies,uid(a),search,pageable(page,size,sort,direction)); }
    @GetMapping("/companies/{id}") public Company getCompany(Authentication a,@PathVariable UUID id) { return owned(companies.findById(id),uid(a)); }
    @PutMapping("/companies/{id}") public Company updateCompany(Authentication a,@PathVariable UUID id,@Valid @RequestBody CompanyDto d) { Company x=owned(companies.findById(id),uid(a)); companyFields(x,d); return companies.save(x); }
    @DeleteMapping("/companies/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteCompany(Authentication a,@PathVariable UUID id) { Company x=owned(companies.findById(id),uid(a)); x.setDeletedAt(OffsetDateTime.now()); companies.save(x); }
    private void companyFields(Company x,CompanyDto d) { x.setName(d.name().trim()); x.setCareerPage(d.careerPage()); x.setPriority(d.priority()); x.setDreamCompany(d.dreamCompany()); x.setApplicationStatus(d.applicationStatus()==null?"NOT_APPLIED":d.applicationStatus()); x.setNotes(d.notes()); }

    @PostMapping("/contacts") @ResponseStatus(HttpStatus.CREATED) public Contact createContact(Authentication a,@Valid @RequestBody ContactDto d) { Contact x=new Contact(); x.setUserId(uid(a)); contactFields(x,d,uid(a)); return contacts.save(x); }
    @GetMapping("/contacts") public Page<Contact> listContacts(Authentication a,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="createdAt") String sort,@RequestParam(defaultValue="desc") String direction,@RequestParam(required=false) String search,@RequestParam(required=false) ContactStatus status) {
        UUID user=uid(a); Pageable p=pageable(page,size,sort,direction);
        Specification<Contact> spec=(root,q,cb)->cb.and(cb.equal(root.get("userId"),user),cb.isNull(root.get("deletedAt")));
        if(status!=null) spec=spec.and((root,q,cb)->cb.equal(root.get("status"),status));
        if(search!=null&&!search.isBlank()) { String like="%"+search.toLowerCase(Locale.ROOT)+"%"; spec=spec.and((root,q,cb)->cb.or(cb.like(cb.lower(root.get("name")),like),cb.like(cb.lower(root.get("notes")),like),cb.like(cb.lower(root.get("designation")),like),cb.like(cb.lower(root.get("email")),like))); }
        return contacts.findAll(spec,p);
    }
    @GetMapping("/contacts/{id}") public Contact getContact(Authentication a,@PathVariable UUID id) { return owned(contacts.findById(id),uid(a)); }
    @PutMapping("/contacts/{id}") public Contact updateContact(Authentication a,@PathVariable UUID id,@Valid @RequestBody ContactDto d) { Contact x=owned(contacts.findById(id),uid(a)); contactFields(x,d,uid(a)); return contacts.save(x); }
    @DeleteMapping("/contacts/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteContact(Authentication a,@PathVariable UUID id) { Contact x=owned(contacts.findById(id),uid(a)); x.setDeletedAt(OffsetDateTime.now()); contacts.save(x); }
    private void contactFields(Contact x,ContactDto d,UUID user) {
        if(d.companyId()!=null) owned(companies.findById(d.companyId()),user);
        x.setName(d.name().trim()); x.setCompanyId(d.companyId()); x.setLinkedinUrl(d.linkedinUrl()); x.setDesignation(d.designation()); x.setLocation(d.location()); x.setEmail(d.email()); x.setSource(d.source());
        x.setDateAdded(d.dateAdded()==null?LocalDate.now():d.dateAdded()); x.setNotes(d.notes()); x.setStatus(d.status()==null?ContactStatus.NOT_CONTACTED:d.status());
    }

    @PostMapping("/resumes") @ResponseStatus(HttpStatus.CREATED) public Resume createResume(Authentication a,@Valid @RequestBody ResumeDto d) { Resume x=new Resume(); x.setUserId(uid(a)); resumeFields(x,d); return resumes.save(x); }
    @GetMapping("/resumes") public Page<Resume> listResumes(Authentication a,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="createdAt") String sort,@RequestParam(defaultValue="desc") String direction) { return page(resumes,uid(a),null,pageable(page,size,sort,direction)); }
    @GetMapping("/resumes/{id}") public Resume getResume(Authentication a,@PathVariable UUID id) { return owned(resumes.findById(id),uid(a)); }
    @PutMapping("/resumes/{id}") public Resume updateResume(Authentication a,@PathVariable UUID id,@Valid @RequestBody ResumeDto d) { Resume x=owned(resumes.findById(id),uid(a)); resumeFields(x,d); return resumes.save(x); }
    @DeleteMapping("/resumes/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteResume(Authentication a,@PathVariable UUID id) { Resume x=owned(resumes.findById(id),uid(a)); x.setDeletedAt(OffsetDateTime.now()); resumes.save(x); }
    private void resumeFields(Resume x,ResumeDto d) { x.setLabel(d.label()); x.setFileName(d.fileName()); x.setStorageKey(d.storageKey()); x.setContentType(d.contentType()); x.setFileSize(d.fileSize()); }

    @PostMapping("/outreach") @ResponseStatus(HttpStatus.CREATED) public Outreach createOutreach(Authentication a,@Valid @RequestBody OutreachDto d) { Outreach x=new Outreach(); x.setUserId(uid(a)); outreachFields(x,d,uid(a)); return outreach.save(x); }
    @GetMapping("/outreach") public Page<Outreach> listOutreach(Authentication a,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="sentAt") String sort,@RequestParam(defaultValue="desc") String direction) { return page(outreach,uid(a),null,pageable(page,size,sort,direction)); }
    @GetMapping("/outreach/{id}") public Outreach getOutreach(Authentication a,@PathVariable UUID id) { return owned(outreach.findById(id),uid(a)); }
    @PutMapping("/outreach/{id}") public Outreach updateOutreach(Authentication a,@PathVariable UUID id,@Valid @RequestBody OutreachDto d) { Outreach x=owned(outreach.findById(id),uid(a)); outreachFields(x,d,uid(a)); return outreach.save(x); }
    @DeleteMapping("/outreach/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteOutreach(Authentication a,@PathVariable UUID id) { Outreach x=owned(outreach.findById(id),uid(a)); x.setDeletedAt(OffsetDateTime.now()); outreach.save(x); }
    private void outreachFields(Outreach x,OutreachDto d,UUID user) { owned(contacts.findById(d.contactId()),user); if(d.resumeId()!=null) owned(resumes.findById(d.resumeId()),user); x.setContactId(d.contactId()); x.setResumeId(d.resumeId()); x.setChannel(d.channel()); x.setMessageVersion(d.messageVersion()); x.setMessageText(d.messageText()); x.setSentAt(d.sentAt()); }

    @PostMapping("/interviews") @ResponseStatus(HttpStatus.CREATED) public Interview createInterview(Authentication a,@Valid @RequestBody InterviewDto d) { Interview x=new Interview(); x.setUserId(uid(a)); interviewFields(x,d,uid(a)); return interviews.save(x); }
    @GetMapping("/interviews") public Page<Interview> listInterviews(Authentication a,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="createdAt") String sort,@RequestParam(defaultValue="desc") String direction) { return page(interviews,uid(a),null,pageable(page,size,sort,direction)); }
    @GetMapping("/interviews/{id}") public Interview getInterview(Authentication a,@PathVariable UUID id) { return owned(interviews.findById(id),uid(a)); }
    @PutMapping("/interviews/{id}") public Interview updateInterview(Authentication a,@PathVariable UUID id,@Valid @RequestBody InterviewDto d) { Interview x=owned(interviews.findById(id),uid(a)); interviewFields(x,d,uid(a)); return interviews.save(x); }
    @DeleteMapping("/interviews/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteInterview(Authentication a,@PathVariable UUID id) { Interview x=owned(interviews.findById(id),uid(a)); x.setDeletedAt(OffsetDateTime.now()); interviews.save(x); }
    private void interviewFields(Interview x,InterviewDto d,UUID user) { owned(companies.findById(d.companyId()),user); x.setCompanyId(d.companyId()); x.setRound(d.round()); x.setInterviewAt(d.interviewAt()); x.setResult(d.result()); x.setFeedback(d.feedback()); x.setPackageAmount(d.packageAmount()); x.setNotes(d.notes()); }
}
