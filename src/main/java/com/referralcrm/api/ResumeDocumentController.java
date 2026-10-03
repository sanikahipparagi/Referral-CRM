package com.referralcrm.api;

import com.referralcrm.service.ResumeExtractionService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/resumes")
@Validated
public class ResumeDocumentController {
    private final ResumeExtractionService extraction;
    public ResumeDocumentController(ResumeExtractionService extraction) { this.extraction=extraction; }
    @PostMapping(value="/upload",consumes="multipart/form-data") @ResponseStatus(HttpStatus.CREATED)
    public ResumeExtractionService.UploadResult upload(Authentication auth,@RequestParam @NotBlank @Size(max=120) String label,@RequestPart("file") MultipartFile file) {
        return extraction.upload((UUID)auth.getPrincipal(),label,file);
    }
    @GetMapping("/{resumeId}/documents") public List<ResumeExtractionService.DocumentView> documents(Authentication auth,@PathVariable UUID resumeId) { return extraction.documents((UUID)auth.getPrincipal(),resumeId); }
    @GetMapping("/{resumeId}/intelligence") public com.referralcrm.service.ResumeProfileService.ProfileView intelligence(Authentication auth,@PathVariable UUID resumeId) { return extraction.intelligence((UUID)auth.getPrincipal(),resumeId); }
    @GetMapping("/{resumeId}/documents/{documentId}/text") public String extractedText(Authentication auth,@PathVariable UUID resumeId,@PathVariable UUID documentId) { return extraction.extractedText((UUID)auth.getPrincipal(),resumeId,documentId); }
    @GetMapping("/{resumeId}/documents/{documentId}/download") public ResponseEntity<byte[]> download(Authentication auth,@PathVariable UUID resumeId,@PathVariable UUID documentId) {
        ResumeExtractionService.Download file=extraction.download((UUID)auth.getPrincipal(),resumeId,documentId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(file.fileName(),java.nio.charset.StandardCharsets.UTF_8).build().toString()).body(file.bytes());
    }
}
