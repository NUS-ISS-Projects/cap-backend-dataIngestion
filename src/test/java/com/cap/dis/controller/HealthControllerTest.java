package com.cap.dis.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthCheck_shouldReturnOkAndExpectedMessage() throws Exception {
        // Arrange
        String expectedHostname = System.getenv("HOSTNAME");
        if (expectedHostname == null) {
            expectedHostname = "null"; // Default if HOSTNAME env var is not set
        }
        String expectedResponseString = "Data ingestion service is up and running on pod: " + expectedHostname;

        // Act
        ResultActions resultActions = mockMvc.perform(get("/api/ingestion/health"));

        // Assert
        resultActions.andExpect(status().isOk())
                     .andExpect(content().string(containsString(expectedResponseString)));
    }
}