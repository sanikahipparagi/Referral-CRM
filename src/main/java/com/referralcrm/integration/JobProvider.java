package com.referralcrm.integration;

import java.util.List;

/** Future port for explicitly imported or user-authorized job data. */
public interface JobProvider {
    List<JobOpportunity> findJobs(String query, String location);
    record JobOpportunity(String title, String company, String location, String publicUrl) {}
}
