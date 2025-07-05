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

    // Stores timestamps of all PDUs received in the last minute
    private final ConcurrentLinkedDeque<Long> pduReceiveTimestamps = new ConcurrentLinkedDeque<>();
    private final AtomicLong lastPduReceivedTimestampMsAtomic = new AtomicLong(0);
    
    // Stores timestamps for specific PDU types
    private final ConcurrentLinkedDeque<Long> entityStatePduTimestamps = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Long> fireEventPduTimestamps = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Long> collisionPduTimestamps = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Long> detonationPduTimestamps = new ConcurrentLinkedDeque<>();
    
    // Stores timestamps for new PDU types
    private final ConcurrentLinkedDeque<Long> dataPduTimestamps = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Long> actionRequestPduTimestamps = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Long> startResumePduTimestamps = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Long> setDataPduTimestamps = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Long> designatorPduTimestamps = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Long> electromagneticEmissionsPduTimestamps = new ConcurrentLinkedDeque<>();

    public void pduReceived() {
        long currentTimeMs = System.currentTimeMillis();
        lastPduReceivedTimestampMsAtomic.set(currentTimeMs);
        pduReceiveTimestamps.addLast(currentTimeMs);

        // Prune old timestamps (optional here, can also be done in getMetrics)
        // For very high rates, pruning here might be better.
        // For simplicity, we'll prune in getMetrics to ensure window is accurate at query time.
    }
    
    public void entityStatePduReceived() {
        pduReceived(); // Update general PDU metrics
        entityStatePduTimestamps.addLast(System.currentTimeMillis());
    }
    
    public void fireEventPduReceived() {
        pduReceived(); // Update general PDU metrics
        fireEventPduTimestamps.addLast(System.currentTimeMillis());
    }
    
    public void collisionPduReceived() {
        pduReceived(); // Update general PDU metrics
        collisionPduTimestamps.addLast(System.currentTimeMillis());
    }
    
    public void detonationPduReceived() {
        pduReceived(); // Update general PDU metrics
        detonationPduTimestamps.addLast(System.currentTimeMillis());
    }
    
    public void dataPduReceived() {
        pduReceived(); // Update general PDU metrics
        dataPduTimestamps.addLast(System.currentTimeMillis());
    }
    
    public void actionRequestPduReceived() {
        pduReceived(); // Update general PDU metrics
        actionRequestPduTimestamps.addLast(System.currentTimeMillis());
    }
    
    public void startResumePduReceived() {
        pduReceived(); // Update general PDU metrics
        startResumePduTimestamps.addLast(System.currentTimeMillis());
    }
    
    public void setDataPduReceived() {
        pduReceived(); // Update general PDU metrics
        setDataPduTimestamps.addLast(System.currentTimeMillis());
    }
    
    public void designatorPduReceived() {
        pduReceived(); // Update general PDU metrics
        designatorPduTimestamps.addLast(System.currentTimeMillis());
    }
    
    public void electromagneticEmissionsPduReceived() {
        pduReceived(); // Update general PDU metrics
        electromagneticEmissionsPduTimestamps.addLast(System.currentTimeMillis());
    }

    public RealTimeMetrics getMetrics() {
        long currentTimeMs = System.currentTimeMillis();
        long sixtySecondsAgo = currentTimeMs - ONE_MINUTE_MS;

        // Prune timestamps older than 60 seconds for all PDU types
        // Iterating from the oldest (head)
        pruneTimestamps(pduReceiveTimestamps, sixtySecondsAgo);
        pruneTimestamps(entityStatePduTimestamps, sixtySecondsAgo);
        pruneTimestamps(fireEventPduTimestamps, sixtySecondsAgo);
        pruneTimestamps(collisionPduTimestamps, sixtySecondsAgo);
        pruneTimestamps(detonationPduTimestamps, sixtySecondsAgo);
        
        // Prune timestamps for new PDU types
        pruneTimestamps(dataPduTimestamps, sixtySecondsAgo);
        pruneTimestamps(actionRequestPduTimestamps, sixtySecondsAgo);
        pruneTimestamps(startResumePduTimestamps, sixtySecondsAgo);
        pruneTimestamps(setDataPduTimestamps, sixtySecondsAgo);
        pruneTimestamps(designatorPduTimestamps, sixtySecondsAgo);
        pruneTimestamps(electromagneticEmissionsPduTimestamps, sixtySecondsAgo);

        long currentPdusInLastSixtySeconds = pduReceiveTimestamps.size();
        long entityStatePdusInLastSixtySeconds = entityStatePduTimestamps.size();
        long fireEventPdusInLastSixtySeconds = fireEventPduTimestamps.size();
        long collisionPdusInLastSixtySeconds = collisionPduTimestamps.size();
        long detonationPdusInLastSixtySeconds = detonationPduTimestamps.size();
        
        // Count new PDU types
        long dataPdusInLastSixtySeconds = dataPduTimestamps.size();
        long actionRequestPdusInLastSixtySeconds = actionRequestPduTimestamps.size();
        long startResumePdusInLastSixtySeconds = startResumePduTimestamps.size();
        long setDataPdusInLastSixtySeconds = setDataPduTimestamps.size();
        long designatorPdusInLastSixtySeconds = designatorPduTimestamps.size();
        long electromagneticEmissionsPdusInLastSixtySeconds = electromagneticEmissionsPduTimestamps.size();
        
        double rate = (double) currentPdusInLastSixtySeconds / 60.0;
        long lastTimestamp = lastPduReceivedTimestampMsAtomic.get();

        if (lastTimestamp == 0 && currentPdusInLastSixtySeconds == 0) {
            lastTimestamp = currentTimeMs; // Avoid returning 0 if no PDUs yet
        }
        
        log.debug("Current metrics: Last PDU at {}, Count in last 60s: {}, Rate: {}/s",
                lastTimestamp, currentPdusInLastSixtySeconds, String.format("%.2f", rate));

        return new RealTimeMetrics(
            lastTimestamp, 
            currentPdusInLastSixtySeconds, 
            rate,
            entityStatePdusInLastSixtySeconds,
            fireEventPdusInLastSixtySeconds,
            collisionPdusInLastSixtySeconds,
            detonationPdusInLastSixtySeconds,
            dataPdusInLastSixtySeconds,
            actionRequestPdusInLastSixtySeconds,
            startResumePdusInLastSixtySeconds,
            setDataPdusInLastSixtySeconds,
            designatorPdusInLastSixtySeconds,
            electromagneticEmissionsPdusInLastSixtySeconds
        );
    }
    
    private void pruneTimestamps(ConcurrentLinkedDeque<Long> timestamps, long cutoffTime) {
        while (timestamps.peekFirst() != null && timestamps.peekFirst() < cutoffTime) {
            timestamps.pollFirst();
        }
    }
}