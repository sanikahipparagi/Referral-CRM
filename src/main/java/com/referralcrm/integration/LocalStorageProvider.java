package com.referralcrm.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalStorageProvider implements ResumeStorageService {
    private final Path root;
    public LocalStorageProvider(@Value("${app.resume-storage-path:./data/resumes}") String root) { this.root=Path.of(root).toAbsolutePath().normalize(); }
    @Override public String store(UUID documentId,byte[] bytes) throws IOException {
        Files.createDirectories(root);
        String key=documentId+".pdf";
        Files.write(resolve(key),bytes,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
        return key;
    }
    @Override public byte[] read(String storagePath) throws IOException { return Files.readAllBytes(resolve(storagePath)); }
    @Override public void delete(String storagePath) throws IOException { Files.deleteIfExists(resolve(storagePath)); }
    private Path resolve(String key) {
        if(key==null||!key.matches("[0-9a-fA-F-]{36}\\.pdf")) throw new IllegalArgumentException("Invalid stored resume key");
        Path resolved=root.resolve(key).normalize();
        if(!resolved.startsWith(root)) throw new IllegalArgumentException("Invalid stored resume key");
        return resolved;
    }
}
