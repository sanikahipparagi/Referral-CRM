package com.referralcrm.integration;

import java.io.IOException;
import java.util.UUID;

/** Storage port for resume binaries. Implementations keep bytes outside PostgreSQL. */
public interface ResumeStorageService {
    String store(UUID documentId, byte[] bytes) throws IOException;
    byte[] read(String storagePath) throws IOException;
    void delete(String storagePath) throws IOException;
}
