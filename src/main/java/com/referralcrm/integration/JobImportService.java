package com.referralcrm.integration;

import java.util.List;
import java.util.UUID;

/** Future import port for manually supplied or explicitly authorized job data. */
public interface JobImportService {
    int importJobs(UUID userId, List<JobProvider.JobOpportunity> opportunities);
}
