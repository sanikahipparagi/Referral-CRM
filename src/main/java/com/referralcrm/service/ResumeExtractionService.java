package com.referralcrm.service;

import com.referralcrm.api.ApiException;
import com.referralcrm.domain.Resume;
import com.referralcrm.domain.ResumeDocument;
import com.referralcrm.integration.ResumeStorageService;
import com.referralcrm.repository.ResumeDocumentRepository;
import com.referralcrm.repository.ResumeRepository;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeExtractionService {
    public record ResumeView(UUID id,String label,String fileName,long fileSize) {}
    public record DocumentView(UUID id,String fileName,String fileType,long fileSize,OffsetDateTime createdAt) {}
    public record Download(String fileName,byte[] bytes) {}
    public record UploadResult(ResumeView resume,DocumentView document,ResumeProfileService.ProfileView intelligence) {}
    private final ResumeRepository resumes; private final ResumeDocumentRepository documents; private final ResumeStorageService storage;
    private final ResumeParserService parser; private final ResumeProfileService profiles; private final long maxBytes;
    public ResumeExtractionService(ResumeRepository resumes,ResumeDocumentRepository documents,ResumeStorageService storage,ResumeParserService parser,ResumeProfileService profiles,
        @org.springframework.beans.factory.annotation.Value("${spring.servlet.multipart.max-file-size:10MB}") DataSize maxFileSize) {
        this.resumes=resumes; this.documents=documents; this.storage=storage; this.parser=parser; this.profiles=profiles; this.maxBytes=maxFileSize.toBytes();
    }

    @Transactional
    public UploadResult upload(UUID userId,String label,MultipartFile file) {
        if(file==null||file.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST,"Choose a PDF file to upload");
        if(file.getSize()>maxBytes) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,"PDF upload exceeds the configured file-size limit");
        String fileName=safeFileName(file.getOriginalFilename());
        if(!fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Only PDF resumes are supported");
        byte[] bytes;
        try { bytes=file.getBytes(); } catch(IOException e) { throw new ApiException(HttpStatus.BAD_REQUEST,"Could not read the uploaded file"); }
        String text=parser.extract(bytes);
        Resume resume=new Resume(); resume.setUserId(userId); resume.setLabel(label.trim()); resume.setFileName(fileName); resume.setStorageKey("pending.pdf"); resume.setContentType("application/pdf"); resume.setFileSize(bytes.length);
        resume=resumes.saveAndFlush(resume);
        ResumeDocument document=new ResumeDocument(); document.setResumeId(resume.getId()); document.setFileName(fileName); document.setFileType("application/pdf"); document.setFileSize(bytes.length); document.setStoragePath("pending.pdf"); document.setExtractedText("");
        document=documents.saveAndFlush(document);
        String key=null;
        try {
            key=storage.store(document.getId(),bytes);
            document.setStoragePath(key); document.setExtractedText(text); documents.save(document);
            resume.setStorageKey(key); resumes.save(resume);
            profiles.extractAndSave(userId,resume.getId(),text);
            return new UploadResult(view(resume),documentView(document),profiles.get(userId,resume.getId(),text));
        } catch(IOException e) {
            if(key!=null) try { storage.delete(key); } catch(IOException ignored) { }
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,"Resume could not be stored");
        } catch(RuntimeException e) {
            if(key!=null) try { storage.delete(key); } catch(IOException ignored) { }
            throw e;
        }
    }
    @Transactional(readOnly=true)
    public List<DocumentView> documents(UUID userId,UUID resumeId) {
        owned(userId,resumeId); return documents.findByResumeIdAndDeletedAtIsNullOrderByCreatedAtDesc(resumeId).stream().map(this::documentView).toList();
    }
    @Transactional(readOnly=true)
    public ResumeProfileService.ProfileView intelligence(UUID userId,UUID resumeId) {
        owned(userId,resumeId); String text=documents.findByResumeIdAndDeletedAtIsNullOrderByCreatedAtDesc(resumeId).stream().findFirst().map(ResumeDocument::getExtractedText).orElse("");
        return profiles.get(userId,resumeId,text);
    }
    @Transactional(readOnly=true)
    public String extractedText(UUID userId,UUID resumeId,UUID documentId) {
        owned(userId,resumeId); return documents.findByIdAndResumeIdAndDeletedAtIsNull(documentId,resumeId).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Resume document not found")).getExtractedText();
    }
    @Transactional(readOnly=true)
    public Download download(UUID userId,UUID resumeId,UUID documentId) {
        owned(userId,resumeId); ResumeDocument document=documents.findByIdAndResumeIdAndDeletedAtIsNull(documentId,resumeId).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Resume document not found"));
        try { return new Download(document.getFileName(),storage.read(document.getStoragePath())); }
        catch(IOException e) { throw new ApiException(HttpStatus.NOT_FOUND,"Stored resume file is unavailable"); }
    }
    private Resume owned(UUID userId,UUID id) { return resumes.findById(id).filter(r->r.getDeletedAt()==null&&r.getUserId().equals(userId)).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Resume not found")); }
    private ResumeView view(Resume resume) { return new ResumeView(resume.getId(),resume.getLabel(),resume.getFileName(),resume.getFileSize()); }
    private DocumentView documentView(ResumeDocument document) { return new DocumentView(document.getId(),document.getFileName(),document.getFileType(),document.getFileSize(),document.getCreatedAt()==null?null:document.getCreatedAt().atOffset(java.time.ZoneOffset.UTC)); }
    private String safeFileName(String input) {
        if(input==null||input.isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST,"Resume filename is required");
        String normalized=input.replace('\\','/'); String name=normalized.substring(normalized.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]","").strip();
        if(name.isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST,"Resume filename is invalid"); return name.length()>255?name.substring(name.length()-255):name;
    }
}
