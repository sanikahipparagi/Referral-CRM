package com.referralcrm.integration;

import java.util.List;

/** Future port for user-initiated contact imports. Never scrapes LinkedIn or automates its UI. */
public interface ContactProvider {
    List<ImportedContact> importContacts(String sourceReference);
    record ImportedContact(String name, String company, String role, String location, String profileUrl) {}
}
