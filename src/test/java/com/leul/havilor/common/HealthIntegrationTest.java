package com.leul.havilor.common;

import com.leul.havilor.FixedClockConfiguration;
import com.leul.havilor.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow 23: /actuator/health is open and says only UP or DOWN; every other Actuator endpoint is off.
// Like AuthIntegrationTest, it doesn't import TestAuthConfiguration, so the requests carry no token.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
class HealthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthIsOpenAndShowsNoDetails() throws Exception {
        String body = mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))   // database and Redis are up (Testcontainers)
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("postgres", "redis", "diskSpace");
    }

    @Test
    void everyOtherActuatorEndpointIsOff() throws Exception {
        for (String path : new String[]{"/actuator", "/actuator/env", "/actuator/beans", "/actuator/metrics",
                "/actuator/info", "/actuator/heapdump", "/actuator/configprops"}) {
            int status = mockMvc.perform(get(path)).andReturn().getResponse().getStatus();
            assertThat(status).as(path).isGreaterThanOrEqualTo(400);
        }
    }
}
