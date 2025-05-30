package com.cap.dis.service;

import com.cap.dis.model.RealTimeMetrics;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;

@Component
public class DisMetricsTracker {

    private final AtomicLong lastPduReceivedTimestamp = new AtomicLong(0);
    // For "pduCountLastMinute", a more sophisticated approach with a sliding window
    // or scheduled aggregation would be needed. For simplicity here, let's track
    // PDUs received since the last call to get metrics or a simple recent count.
    private final AtomicLong pduReceivedCounter = new AtomicLong(0);


    public void pduReceived() {
        lastPduReceivedTimestamp.set(System.currentTimeMillis());
        pduReceivedCounter.incrementAndGet();
    }

    public RealTimeMetrics getMetrics() {
        // This is a simplified example. In a real scenario, you might want to
        // implement a sliding window for "pduCountLastMinute" or similar.
        // Here, pduCountLastProcessingCycle will be the count since the last reset/query.
        long currentPduCount = pduReceivedCounter.getAndSet(0); // Reset count after fetching
        long lastTimestamp = lastPduReceivedTimestamp.get();
        if (lastTimestamp == 0 && currentPduCount == 0) { // If no PDUs ever, report current time as "last seen" to avoid zero.
            lastTimestamp = System.currentTimeMillis();
        }
        return new RealTimeMetrics(lastTimestamp, currentPduCount);
    }
}