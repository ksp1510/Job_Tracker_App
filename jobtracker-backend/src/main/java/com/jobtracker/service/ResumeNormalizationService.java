package com.jobtracker.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResumeNormalizationService {

    public List<String> normalizeList(List<String> items, int max) {
        if (items == null) return List.of();
        return items.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(this::canonicalize)
                .distinct()
                .limit(max)
                .collect(Collectors.toList());
    }

    public String canonicalize(String s) {
        // minimal canonicalization (extend later)
        String x = s.trim();
        x = x.replaceAll("\\s+", " ");
        return x;
    }

    public List<String> mergeLists(List<String>... lists) {
        List<String> out = new ArrayList<>();
        if (lists != null) {
            for (List<String> l : lists) {
                if (l != null) out.addAll(l);
            }
        }
        return out;
    }
}
