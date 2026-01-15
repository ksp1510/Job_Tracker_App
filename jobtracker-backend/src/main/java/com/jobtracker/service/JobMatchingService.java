package com.jobtracker.service;

import com.jobtracker.dto.jobmatch.JobMatchResult;
import com.jobtracker.model.CandidateProfile;
import com.jobtracker.model.JobListing;
import com.jobtracker.repository.JobListingRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobMatchingService {

    private final JobListingRepository jobListingRepository;

    private static final Set<String> STOPWORDS = Set.of(
        "a","an","and","or","the","to","of","in","on","for","with","as","at","by","from",
        "is","are","was","were","be","been","being",
        "this","that","these","those",
        "we","our","you","your","they","their","them",
        "will","can","may","must","should","could","would",

        // generic resume/job filler (job-agnostic)
        "across","communication","skilled","experienced","experience",
        "strong","excellent","good","great",
        "team","member","work","working",
        "role","responsibilities","requirements","required",
        "skills","skill","years","year",
        "technical","technologies","technology",
        "user","customers","customer",
        "variety","functional","cross","two",

        // common verbs that add noise
        "using","used","use","develop","developed","design","designed",
        "implement","implemented","provide","provided","support","supporting"
    );




    public List<JobMatchResult> matchTopJobs(CandidateProfile profile, int limit) {
        int pool = Math.min(Math.max(limit * 30, 200), 2000);
        Pageable p = PageRequest.of(0, pool, Sort.by(Sort.Direction.DESC, "postedDate"));

        List<JobListing> jobs = jobListingRepository.findByIsActiveTrue(p);

        // Dedupe (externalId preferred, fallback to title+company+location)
        Map<String, JobListing> unique = new LinkedHashMap<>();
        for (JobListing j : jobs) {
            String key = (j.getExternalId() != null && !j.getExternalId().isBlank())
                    ? "ext:" + j.getExternalId()
                    : "tcl:" + norm(j.getTitle()) + "|" + norm(j.getCompany()) + "|" + norm(j.getLocation());
            unique.putIfAbsent(key, j);
        }

        return unique.values().stream()
                .map(j -> score(profile, j))
                .sorted(Comparator.comparingDouble(JobMatchResult::getScore).reversed())
                .limit(limit)
                .toList();
    }


    private JobMatchResult score(CandidateProfile p, JobListing j) {
        // Candidate terms
        Set<String> candidate = new HashSet<>();
        candidate.addAll(toSet(p.getSkills(), p.getTechnologies(), p.getTitles()));
        addTokens(candidate, p.getSummary());

        // Job primary terms (high signal)
        Set<String> jobPrimary = new HashSet<>();
        addTokens(jobPrimary, j.getTitle());
        addTokens(jobPrimary, j.getCompany());
        addTokens(jobPrimary, j.getLocation());
        addTokens(jobPrimary, j.getJobType());
        addTokens(jobPrimary, j.getExperienceLevel());
        if (j.getSkills() != null) j.getSkills().forEach(s -> addTokens(jobPrimary, s));

        // Job full terms (includes description)
        Set<String> jobAll = new HashSet<>(jobPrimary);
        addTokens(jobAll, j.getDescription());

        // Matched keywords based on full set (for display)
        Set<String> matched = new HashSet<>(candidate);
        matched.retainAll(jobAll);

        // Skill-specific match (only if job.skills exists)
        List<String> jobSkills = j.getSkills() == null ? List.of() : j.getSkills();
        Set<String> jobSkillsSet = jobSkills.stream()
                .map(this::norm)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        List<String> matchedSkills = jobSkillsSet.stream().filter(candidate::contains).sorted().toList();
        List<String> missingSkills = jobSkillsSet.stream().filter(s -> !candidate.contains(s)).sorted().toList();

        // F1(primary)
        double f1Primary = f1(candidate, jobPrimary);

        // F1(all)
        double f1All = f1(candidate, jobAll);

        // Small bump from explicit skills list if present
        double skillOverlap = jobSkillsSet.isEmpty() ? 0.0
                : ((double) matchedSkills.size() / (double) jobSkillsSet.size());

        // Weighted score: prioritize title/skills matching
        double score = 0.65 * f1Primary + 0.25 * f1All + 0.10 * skillOverlap;

        return new JobMatchResult(
                j.getId(),
                j.getTitle(),
                j.getCompany(),
                j.getLocation(),
                j.isRemote(),
                round3(score),
                matchedSkills,
                missingSkills,
                matched.stream().sorted().limit(30).toList()
        );
    }

    private double f1(Set<String> candidate, Set<String> job) {
        if (candidate.isEmpty() || job.isEmpty()) return 0.0;

        Set<String> inter = new HashSet<>(candidate);
        inter.retainAll(job);

        double precision = (double) inter.size() / (double) job.size();
        double recall = (double) inter.size() / (double) candidate.size();

        return (precision + recall) == 0.0 ? 0.0 : (2.0 * precision * recall) / (precision + recall);
    }



private Set<String> intersection(Set<String> a, Set<String> b) {
    if (a.isEmpty() || b.isEmpty()) return Set.of();
    Set<String> x = new HashSet<>(a);
    x.retainAll(b);
    return x;
}


    private Set<String> toSet(List<String>... lists) {
        Set<String> s = new HashSet<>();
        if (lists != null) {
            for (List<String> l : lists) {
                if (l == null) continue;
                for (String x : l) {
                    if (x == null) continue;
                    String n = norm(x);
                    if (!n.isBlank()) s.add(n);
                }
            }
        }
        return s;
    }

    private List<String> tokensFromText(String text) {
        if (text == null) return List.of();
        Set<String> out = new HashSet<>();
        addTokens(out, text);
        return new ArrayList<>(out);
    }

    private double titleSimilarity(List<String> candidateTitles, String jobTitle) {
        if (candidateTitles == null || candidateTitles.isEmpty() || jobTitle == null) return 0.0;

        Set<String> jobTitleTokens = new HashSet<>();
        addTokens(jobTitleTokens, jobTitle);

        double best = 0.0;
        for (String t : candidateTitles) {
            if (t == null) continue;
            Set<String> candTitleTokens = new HashSet<>();
            addTokens(candTitleTokens, t);
            best = Math.max(best, jaccard(candTitleTokens, jobTitleTokens));
        }
        return best;
    }


    private void addTokens(Set<String> out, String text) {
        if (text == null || text.isBlank()) return;

        // Split on non-word-ish characters but keep + # . for tech tokens and dotted words
        String[] parts = text.split("[^A-Za-z0-9+#.]+");

        for (String raw : parts) {
            String n = norm(raw);
            if (n.isBlank()) continue;
            if (n.length() < 2) continue;
            if (STOPWORDS.contains(n)) continue;
            out.add(n);
        }
    }

    private String norm(String s) {
        if (s == null) return "";
        String x = s.trim().toLowerCase(Locale.ROOT);

        // remove leading/trailing non-token chars (but allow + # . inside token)
        x = x.replaceAll("^[^a-z0-9+#.]+|[^a-z0-9+#.]+$", "");

        // specifically strip trailing/leading dots (fix "skills.")
        x = x.replaceAll("^\\.+|\\.+$", "");

        // normalize a few common forms
        if (x.equals(".net") || x.equals("net")) x = "dotnet";

        return x;
    }


    private double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) inter.size() / (double) union.size();
    }

    private double round3(double x) {
        return Math.round(x * 1000.0) / 1000.0;
    }
}
