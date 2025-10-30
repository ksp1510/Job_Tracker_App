package com.jobtracker.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class SalaryDeserializer extends JsonDeserializer<Double> {

    @Override
    public Double deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();

        if (value == null || value.isBlank()) {
            return null;
        }

        // Remove all symbols except digits, dots, and dashes
        String clean = value.replaceAll("[^0-9.\\-]", "").trim();

        if (clean.isEmpty()) return null;

        try {
            if (clean.contains("-")) {
                String[] parts = clean.split("-");
                double min = Double.parseDouble(parts[0]);
                double max = Double.parseDouble(parts[1]);
                return (min + max) / 2; // take the average
            }
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            return null; // gracefully ignore malformed numbers
        }
    }
}
