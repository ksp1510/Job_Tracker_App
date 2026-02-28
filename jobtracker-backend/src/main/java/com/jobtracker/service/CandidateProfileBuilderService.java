package com.jobtracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.dto.resume.ResumeStructuredData;
import com.jobtracker.model.CandidateProfile;
import com.jobtracker.model.ParsedResume;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CandidateProfileBuilderService {

    private final ResumeNormalizationService normalizationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CandidateProfile build(String userId, String resumeFileId, ParsedResume parsed, ResumeStructuredData data) {
        CandidateProfile p = new CandidateProfile();
        p.setUserId(userId);
        p.setResumeFileId(resumeFileId);
        p.setParsedResumeId(parsed == null ? null : parsed.getId());
        p.setParserVersion("v2");
        p.setGeneratedAt(Instant.now());

        if (data != null && data.getBasics() != null) {
            p.setFullName(data.getBasics().getFullName());
            p.setEmail(data.getBasics().getEmail());
            p.setPhone(data.getBasics().getPhone());

            if (data.getBasics().getLocation() != null) {
                p.setCity(data.getBasics().getLocation().getCity());
                p.setRegion(data.getBasics().getLocation().getRegion());
                p.setCountry(data.getBasics().getLocation().getCountry());
            }
        }

        p.setSummary(data == null ? null : data.getSummary());

        // Titles
        List<String> titlesRaw = new ArrayList<>();
        if (data != null && data.getExperience() != null) {
            for (ResumeStructuredData.Experience e : data.getExperience()) {
                if (e != null && e.getTitle() != null) titlesRaw.add(e.getTitle());
            }
        }
        p.setTitles(normalizationService.normalizeList(titlesRaw, 30));

        // Skills flattened
        List<String> allSkills = normalizationService.mergeLists(
                data != null && data.getSkills() != null ? data.getSkills().getPrimary() : null,
                data != null && data.getSkills() != null ? data.getSkills().getSecondary() : null,
                data != null && data.getSkills() != null ? data.getSkills().getTools() : null,
                data != null && data.getSkills() != null ? data.getSkills().getLanguages() : null
        );
        p.setSkills(normalizationService.normalizeList(allSkills, 120));

        // Technologies from experience/projects
        List<String> techRaw = new ArrayList<>();
        if (data != null && data.getExperience() != null) {
            for (ResumeStructuredData.Experience e : data.getExperience()) {
                if (e != null && e.getTechnologies() != null) techRaw.addAll(e.getTechnologies());
            }
        }
        if (data != null && data.getProjects() != null) {
            for (ResumeStructuredData.Project pr : data.getProjects()) {
                if (pr != null && pr.getTechnologies() != null) techRaw.addAll(pr.getTechnologies());
            }
        }
        p.setTechnologies(normalizationService.normalizeList(techRaw, 120));

        // Total experience months (best-effort)
        p.setTotalExperienceMonths(estimateExperienceMonths(data));

        // store structured JSON for debugging
        try {
            p.setStructuredJson(objectMapper.writeValueAsString(data));
        } catch (Exception ignored) {}

        return p;
    }

    private Integer estimateExperienceMonths(ResumeStructuredData data) {
        if (data == null || data.getExperience() == null) return null;

        int months = 0;
        for (ResumeStructuredData.Experience e : data.getExperience()) {
            if (e == null) continue;
            String s = e.getStartDate();
            String end = e.getEndDate();

            YearMonth start = parseYm(s);
            YearMonth finish = "PRESENT".equalsIgnoreCase(end) ? YearMonth.now() : parseYm(end);

            if (start != null && finish != null && !finish.isBefore(start)) {
                months += (finish.getYear() - start.getYear()) * 12 + (finish.getMonthValue() - start.getMonthValue()) + 1;
            }
        }
        return months == 0 ? null : months;
    }

    private YearMonth parseYm(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return YearMonth.parse(s);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
