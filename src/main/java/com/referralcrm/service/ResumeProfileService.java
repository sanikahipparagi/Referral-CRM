package com.referralcrm.service;

import com.referralcrm.api.ApiException;
import com.referralcrm.domain.*;
import com.referralcrm.repository.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Extracts useful, reviewable resume structure using conservative local heuristics. */
@Service
public class ResumeProfileService {
    public record ProjectView(UUID id,String name,String description,Set<String> technologies) {}
    public record ExperienceView(UUID id,String company,String role,String description,String duration) {}
    public record ProfileView(UUID resumeId,String resumeLabel,String extractedText,List<String> skills,List<ProjectView> projects,List<ExperienceView> experience) {}
    private static final List<String> SKILL_CATALOG=List.of("Java","Kotlin","Python","JavaScript","TypeScript","C++","C#","Go","Rust","SQL","PostgreSQL","MySQL","MongoDB","Redis","Spring","Spring Boot","Hibernate","JPA","REST APIs","GraphQL","Microservices","Kafka","Kafka Streams","RabbitMQ","AWS","Amazon Web Services","Lambda","S3","EC2","Azure","GCP","Docker","Kubernetes","Terraform","Jenkins","GitHub Actions","CI/CD","Linux","React","Next.js","Node.js","Angular","HTML","CSS","Tailwind CSS","Machine Learning","Deep Learning","LLM","Generative AI","OpenAI","PyTorch","TensorFlow","Pandas","Spark","Apache Spark","Airflow","Data Engineering","System Design","Distributed Systems","Payments","Payment Processing","Agile","Scrum");
    private static final Pattern EXPERIENCE_HEADING=Pattern.compile("(?i)^(professional |work |employment )?experience(s)?$|^career history$|^employment history$");
    private static final Pattern PROJECT_HEADING=Pattern.compile("(?i)^(personal |selected )?projects?( experience)?$");
    private static final Pattern SECTION_HEADING=Pattern.compile("(?i)^(skills?|technical skills|competencies|professional summary|summary|education|certifications?|languages|awards|publications|interests|references|projects?( experience)?|(?:professional |work |employment )?experiences?|career history|employment history)$");
    private final ResumeRepository resumes; private final ResumeSkillService skills; private final ResumeProjectRepository projects; private final ResumeExperienceRepository experiences;
    public ResumeProfileService(ResumeRepository resumes,ResumeSkillService skills,ResumeProjectRepository projects,ResumeExperienceRepository experiences) { this.resumes=resumes; this.skills=skills; this.projects=projects; this.experiences=experiences; }

    @Transactional
    public void extractAndSave(UUID userId,UUID resumeId,String text) {
        owned(userId,resumeId);
        List<String> skillMatches=extractSkills(text);
        skills.replace(userId,resumeId,skillMatches);
        List<ResumeProject> oldProjects=projects.findByResumeIdAndDeletedAtIsNullOrderByCreatedAtDesc(resumeId); oldProjects.forEach(p->p.setDeletedAt(OffsetDateTime.now())); projects.saveAll(oldProjects);
        List<ResumeExperience> oldExperience=experiences.findByResumeIdAndDeletedAtIsNullOrderByCreatedAtDesc(resumeId); oldExperience.forEach(e->e.setDeletedAt(OffsetDateTime.now())); experiences.saveAll(oldExperience);
        projects.saveAll(parseProjects(section(text,PROJECT_HEADING),resumeId));
        experiences.saveAll(parseExperience(section(text,EXPERIENCE_HEADING),resumeId));
    }
    public List<String> extractSkills(String text) { return SKILL_CATALOG.stream().filter(s->contains(text,s)).toList(); }
    public ProfileView get(UUID userId,UUID resumeId,String text) {
        Resume resume=owned(userId,resumeId);
        return new ProfileView(resumeId,resume.getLabel(),text,skills.list(userId,resumeId).stream().map(ResumeSkillService.SkillView::skill).toList(),
            projects.findByResumeIdAndDeletedAtIsNullOrderByCreatedAtDesc(resumeId).stream().map(p->new ProjectView(p.getId(),p.getName(),p.getDescription(),p.getTechnologies())).toList(),
            experiences.findByResumeIdAndDeletedAtIsNullOrderByCreatedAtDesc(resumeId).stream().map(e->new ExperienceView(e.getId(),e.getCompany(),e.getRole(),e.getDescription(),e.getDuration())).toList());
    }
    private List<ResumeProject> parseProjects(String section,UUID resumeId) {
        List<ResumeProject> result=new ArrayList<>();
        for(String block:blocks(section)) {
            List<String> lines=Arrays.stream(block.split("\\n")).map(String::strip).filter(s->!s.isBlank()).toList(); if(lines.isEmpty()) continue;
            String name=cleanBullet(lines.getFirst()); if(name.length()>200) name=name.substring(0,200);
            String description=lines.size()>1?String.join(" ",lines.subList(1,lines.size())):"";
            ResumeProject project=new ResumeProject(); project.setResumeId(resumeId); project.setName(name); project.setDescription(description.length()>4000?description.substring(0,4000):description);
            project.setTechnologies(SKILL_CATALOG.stream().filter(s->lines.stream().anyMatch(line->contains(line,s))).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
            result.add(project); if(result.size()==20) break;
        }
        return result;
    }
    private List<ResumeExperience> parseExperience(String section,UUID resumeId) {
        List<ResumeExperience> result=new ArrayList<>();
        for(String block:blocks(section)) {
            List<String> lines=Arrays.stream(block.split("\\n")).map(String::strip).filter(s->!s.isBlank()).toList(); if(lines.isEmpty()) continue;
            List<String> content=lines.stream().filter(line->!line.matches("(?i).*\\b(19|20)\\d{2}\\b.*(?:present|current|\\d{4}).*")).toList();
            String header=cleanBullet(content.isEmpty()?lines.getFirst():content.getFirst()); String role=header,company="";
            String[] parts=header.split("\\s+[|·–—]\\s+",2); if(parts.length==2){role=parts[0];company=parts[1];}
            if(company.isBlank()&&content.size()>1&&content.get(1).length()<120&&!content.get(1).startsWith("-")){company=cleanBullet(content.get(1));}
            String duration=lines.stream().filter(line->line.matches("(?i).*\\b(19|20)\\d{2}\\b.*")).findFirst().orElse(null);
            String description=content.size()>1?String.join(" ",content.subList(1,content.size())):""; if(description.length()>5000)description=description.substring(0,5000);
            ResumeExperience experience=new ResumeExperience(); experience.setResumeId(resumeId); experience.setRole(limit(role,200)); experience.setCompany(limit(company,200)); experience.setDescription(description); experience.setDuration(duration==null?null:limit(duration,120));
            result.add(experience); if(result.size()==20) break;
        }
        return result;
    }
    private String section(String text,Pattern target) {
        String[] lines=text.split("\\n"); int start=-1;
        for(int i=0;i<lines.length;i++) if(target.matcher(lines[i].strip().replaceAll("[:：]$","")).matches()){start=i+1;break;}
        if(start<0)return ""; StringBuilder out=new StringBuilder();
        for(int i=start;i<lines.length;i++){String line=lines[i].strip();if(SECTION_HEADING.matcher(line.replaceAll("[:：]$","")).matches())break;out.append(lines[i]).append('\n');}
        return out.toString().strip();
    }
    private List<String> blocks(String section) { return Arrays.stream(section.split("\\n\\s*\\n")).map(String::strip).filter(s->!s.isBlank()).toList(); }
    private boolean contains(String text,String skill) { return Pattern.compile("(?i)(?<![a-z0-9])"+Pattern.quote(skill)+"(?![a-z0-9])").matcher(Objects.toString(text,"")).find(); }
    private String cleanBullet(String value) { return value.replaceFirst("^[•▪◦*\\-\\d.)\\s]+","").strip(); }
    private String limit(String value,int length) { return value.length()>length?value.substring(0,length):value; }
    private Resume owned(UUID userId,UUID resumeId) { return resumes.findById(resumeId).filter(r->r.getDeletedAt()==null&&r.getUserId().equals(userId)).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Resume not found")); }
}
