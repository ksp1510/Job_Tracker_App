package com.jobtracker.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class ApiUsageTracker {
    
    private final MeterRegistry meterRegistry;
    private final Counter serpApiCalls;
    private final Counter rapidApiCalls;
    
    public ApiUsageTracker(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.serpApiCalls = Counter.builder("api.calls")
            .tag("provider", "serpapi")
            .register(meterRegistry);
        this.rapidApiCalls = Counter.builder("api.calls")
            .tag("provider", "rapidapi")
            .register(meterRegistry);
    }
    
    public void trackApiCall(String provider) {
        meterRegistry.counter("api.calls", "provider", provider).increment();
    }
}
