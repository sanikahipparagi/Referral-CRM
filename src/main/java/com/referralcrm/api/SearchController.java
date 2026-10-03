package com.referralcrm.api;

import com.referralcrm.repository.OutreachRepository;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {
    private final OutreachRepository outreach;
    public SearchController(OutreachRepository outreach) { this.outreach = outreach; }

    @GetMapping
    public List<OutreachRepository.SearchRecord> search(Authentication authentication,
            @RequestParam @Size(min = 2, max = 120) String q) {
        return outreach.searchAll((UUID) authentication.getPrincipal(), q.trim());
    }
}
