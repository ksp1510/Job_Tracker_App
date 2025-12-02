package com.jobtracker.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class ApiUsageTracker {
    
    private final MeterRegistry meterRegistry;
    @SuppressWarnings("unused")
    private final Counter rapidApiCalls;
    
    public ApiUsageTracker(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.rapidApiCalls = Counter.builder("api.calls")
            .tag("provider", "rapidapi")
            .register(meterRegistry);
    }
    
    public void trackApiCall(String provider) {
        meterRegistry.counter("api.calls", "provider", provider).increment();
    }
}
