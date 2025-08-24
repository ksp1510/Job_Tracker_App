package com.jobtracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import software.amazon.awssdk.services.ses.SesClient;

@SpringBootTest
class JobtrackerBackendApplicationTests {

    @MockBean
    private SesClient sesClient;  // ✅ replaces the real bean with a mock

    @Test
    void contextLoads() {
        // no AWS calls made
    }
}
