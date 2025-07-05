package com.cap.dis.controller;

import com.cap.dis.model.RealTimeMetrics;
import com.cap.dis.service.DisMetricsTracker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;

@WebMvcTest(InternalMetricsController.class)
class InternalMetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DisMetricsTracker metricsTracker;

    @Autowired
    private ObjectMapper objectMapper; // For comparing JSON objects if needed

    @Test
    void getRealTimeDisMetrics_shouldReturnMetricsFromTracker() throws Exception {
        // Arrange
        long currentTime = System.currentTimeMillis();
        RealTimeMetrics expectedMetrics = new RealTimeMetrics(
            currentTime,  // lastPduReceivedTimestampMs
            120L,         // pdusInLastSixtySeconds
            2.0,          // averagePduRatePerSecondLastSixtySeconds
            50L,          // entityStatePdusInLastSixtySeconds
            30L,          // fireEventPdusInLastSixtySeconds
            20L,          // collisionPdusInLastSixtySeconds
            20L           // detonationPdusInLastSixtySeconds
        );
        when(metricsTracker.getMetrics()).thenReturn(expectedMetrics);

        // Act
        ResultActions resultActions = mockMvc.perform(get("/internal/metrics/realtime"));

        // Assert
        resultActions.andExpect(status().isOk())
                     .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                     .andExpect(jsonPath("$.lastPduReceivedTimestampMs", is(expectedMetrics.getLastPduReceivedTimestampMs())))
                     .andExpect(jsonPath("$.pdusInLastSixtySeconds", is(expectedMetrics.getPdusInLastSixtySeconds()), Long.class))
                     .andExpect(jsonPath("$.averagePduRatePerSecondLastSixtySeconds", is(expectedMetrics.getAveragePduRatePerSecondLastSixtySeconds())))
                     .andExpect(jsonPath("$.entityStatePdusInLastSixtySeconds", is(expectedMetrics.getEntityStatePdusInLastSixtySeconds()), Long.class))
                     .andExpect(jsonPath("$.fireEventPdusInLastSixtySeconds", is(expectedMetrics.getFireEventPdusInLastSixtySeconds()), Long.class))
                     .andExpect(jsonPath("$.collisionPdusInLastSixtySeconds", is(expectedMetrics.getCollisionPdusInLastSixtySeconds()), Long.class))
                     .andExpect(jsonPath("$.detonationPdusInLastSixtySeconds", is(expectedMetrics.getDetonationPdusInLastSixtySeconds()), Long.class));
    }
}