package com.cap.dis.service;

import com.cap.dis.model.RealTimeMetrics;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DisMetricsTracker {

    private static final Logger log = LoggerFactory.getLogger(DisMetricsTracker.class);
    private static final long ONE_MINUTE_MS = 60 * 1000;

    // Stores timestamps of PDUs received in the last minute (approx)
    private final ConcurrentLinkedDeque<Long> pduReceiveTimestamps = new ConcurrentLinkedDeque<>();
    private final AtomicLong lastPduReceivedTimestampMsAtomic = new AtomicLong(0);

    public void pduReceived() {
        long currentTimeMs = System.currentTimeMillis();
        lastPduReceivedTimestampMsAtomic.set(currentTimeMs);
        pduReceiveTimestamps.addLast(currentTimeMs);

        // Prune old timestamps (optional here, can also be done in getMetrics)
        // For very high rates, pruning here might be better.
        // For simplicity, we'll prune in getMetrics to ensure window is accurate at query time.
    }

    public RealTimeMetrics getMetrics() {
        long currentTimeMs = System.currentTimeMillis();
        long sixtySecondsAgo = currentTimeMs - ONE_MINUTE_MS;

        // Prune timestamps older than 60 seconds
        // Iterating from the oldest (head)
        while (pduReceiveTimestamps.peekFirst() != null && pduReceiveTimestamps.peekFirst() < sixtySecondsAgo) {
            pduReceiveTimestamps.pollFirst();
        }

        long currentPdusInLastSixtySeconds = pduReceiveTimestamps.size();
        double rate = (double) currentPdusInLastSixtySeconds / 60.0;
        long lastTimestamp = lastPduReceivedTimestampMsAtomic.get();

        if (lastTimestamp == 0 && currentPdusInLastSixtySeconds == 0) {
            lastTimestamp = currentTimeMs; // Avoid returning 0 if no PDUs yet
        }
        
        log.debug("Current metrics: Last PDU at {}, Count in last 60s: {}, Rate: {}/s",
                lastTimestamp, currentPdusInLastSixtySeconds, String.format("%.2f", rate));

        return new RealTimeMetrics(lastTimestamp, currentPdusInLastSixtySeconds, rate);
    }
}