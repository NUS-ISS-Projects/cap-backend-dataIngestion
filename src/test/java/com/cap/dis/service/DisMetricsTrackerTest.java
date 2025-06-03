package com.cap.dis.service;

import com.cap.dis.model.RealTimeMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.TimeUnit;

class DisMetricsTrackerTest {

    private DisMetricsTracker metricsTracker;
    private static final long ONE_MINUTE_MS = 60 * 1000; // [cite: 135]

    @BeforeEach
    void setUp() {
        metricsTracker = new DisMetricsTracker();
    }

    @Test
    void testPduReceived_updatesTimestampAndAddsToDeque() {
        long startTime = System.currentTimeMillis();
        metricsTracker.pduReceived(); // [cite: 137]
        long endTime = System.currentTimeMillis();

        // metricsTracker.getMetrics() will prune and calculate counts
        RealTimeMetrics metrics = metricsTracker.getMetrics();

        assertTrue(metrics.getLastPduReceivedTimestampMs() >= startTime && metrics.getLastPduReceivedTimestampMs() <= endTime,
                "Last PDU timestamp should be current time");
        // The pduReceiveTimestamps deque is an internal detail. We verify its effect through getMetrics().
        assertEquals(1, metrics.getPdusInLastSixtySeconds(), "PDU count should be 1 after one PDU");
    }

    @Test
    void testGetMetrics_noPduReceived() {
        long currentTimeBeforeGetMetrics = System.currentTimeMillis();
        RealTimeMetrics metrics = metricsTracker.getMetrics();
        long currentTimeAfterGetMetrics = System.currentTimeMillis();

        // lastPduReceivedTimestampMsAtomic is initialized to 0 [cite: 136]
        // and updated to currentTimeMs in getMetrics if it was 0 and no PDUs were received. [cite: 143]
        assertTrue(metrics.getLastPduReceivedTimestampMs() >= currentTimeBeforeGetMetrics && metrics.getLastPduReceivedTimestampMs() <= currentTimeAfterGetMetrics,
                   "Timestamp should be current time if no PDUs and it was 0 initially");
        assertEquals(0, metrics.getPdusInLastSixtySeconds(), "PDU count should be 0");
        assertEquals(0.0, metrics.getAveragePduRatePerSecondLastSixtySeconds(), "Average rate should be 0.0");
    }

    @Test
    void testGetMetrics_multiplePdusWithinSixtySeconds() throws InterruptedException {
        metricsTracker.pduReceived(); // [cite: 137]
        TimeUnit.MILLISECONDS.sleep(100); // Simulate small delay
        metricsTracker.pduReceived(); // [cite: 137]
        TimeUnit.MILLISECONDS.sleep(100);
        metricsTracker.pduReceived(); // [cite: 137]

        RealTimeMetrics metrics = metricsTracker.getMetrics();

        assertEquals(3, metrics.getPdusInLastSixtySeconds(), "PDU count should be 3");
        assertEquals(3.0 / 60.0, metrics.getAveragePduRatePerSecondLastSixtySeconds(), "Average rate should be 3/60");
    }

    @Test
    void testGetMetrics_pduPruningOlderThanSixtySeconds() throws InterruptedException {
        // Record first PDU
        metricsTracker.pduReceived(); // [cite: 137]
        long firstPduTimestamp = metricsTracker.getMetrics().getLastPduReceivedTimestampMs();

        // Wait for more than 60 seconds
        TimeUnit.MILLISECONDS.sleep(ONE_MINUTE_MS + 1000); // e.g., 61 seconds

        // Record second PDU
        metricsTracker.pduReceived(); // [cite: 137]
        long secondPduTimestamp = metricsTracker.getMetrics().getLastPduReceivedTimestampMs();

        RealTimeMetrics metrics = metricsTracker.getMetrics();

        // The first PDU should have been pruned [cite: 141]
        assertEquals(1, metrics.getPdusInLastSixtySeconds(), "Only the second PDU should be counted as the first is older than 60s");
        assertEquals(1.0 / 60.0, metrics.getAveragePduRatePerSecondLastSixtySeconds());
        assertEquals(secondPduTimestamp, metrics.getLastPduReceivedTimestampMs(), "Last PDU timestamp should be from the second PDU");
        assertTrue(secondPduTimestamp > firstPduTimestamp, "Second PDU must be later than the first");
    }


    @Test
    void testGetMetrics_allPdusOlderThanSixtySeconds() throws InterruptedException {
        // Record a PDU
        metricsTracker.pduReceived(); // [cite: 137]
        long pduTimestamp = metricsTracker.getMetrics().getLastPduReceivedTimestampMs();

        // Wait for more than 60 seconds so all PDUs become "old"
        TimeUnit.MILLISECONDS.sleep(ONE_MINUTE_MS + 1000); // e.g., 61 seconds

        RealTimeMetrics metrics = metricsTracker.getMetrics();

        assertEquals(0, metrics.getPdusInLastSixtySeconds(), "PDU count should be 0 as all PDUs are older than 60s");
        assertEquals(0.0, metrics.getAveragePduRatePerSecondLastSixtySeconds(), "Rate should be 0.0");
        // The lastPduReceivedTimestampMsAtomic should still hold the actual timestamp of the last PDU received [cite: 142]
        assertEquals(pduTimestamp, metrics.getLastPduReceivedTimestampMs(), "Last PDU timestamp should reflect the actual last PDU, even if old");
    }
}