package com.cap.dis.service;

import com.cap.dis.model.RealTimeMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.TimeUnit;

class DisMetricsTrackerDesignatorTest {

    private DisMetricsTracker metricsTracker;
    private static final long ONE_MINUTE_MS = 60 * 1000;

    @BeforeEach
    void setUp() {
        metricsTracker = new DisMetricsTracker();
    }

    @Test
    void testDesignatorPduReceived_updatesMetrics() {
        // Act
        metricsTracker.designatorPduReceived();
        
        // Assert
        RealTimeMetrics metrics = metricsTracker.getMetrics();
        
        // Check total PDU count (should be 1)
        assertEquals(1, metrics.getPdusInLastSixtySeconds(), "Total PDU count should be 1");
        
        // Check designator PDU count
        assertEquals(1, metrics.getDesignatorPdusInLastSixtySeconds(), "Designator PDU count should be 1");
        
        // Check that other PDU type counts are 0
        assertEquals(0, metrics.getEntityStatePdusInLastSixtySeconds(), "Entity state PDU count should be 0");
        assertEquals(0, metrics.getFireEventPdusInLastSixtySeconds(), "Fire event PDU count should be 0");
        assertEquals(0, metrics.getCollisionPdusInLastSixtySeconds(), "Collision PDU count should be 0");
        assertEquals(0, metrics.getDetonationPdusInLastSixtySeconds(), "Detonation PDU count should be 0");
    }
    
    @Test
    void testDesignatorPduPruning() throws InterruptedException {
        // Record a designator PDU
        metricsTracker.designatorPduReceived();
        
        // Wait for more than 60 seconds
        TimeUnit.MILLISECONDS.sleep(ONE_MINUTE_MS + 1000); // e.g., 61 seconds
        
        // Record a different PDU type after the wait
        metricsTracker.entityStatePduReceived();
        
        RealTimeMetrics metrics = metricsTracker.getMetrics();
        
        // Check total PDU count (should be 1, as the first should be pruned)
        assertEquals(1, metrics.getPdusInLastSixtySeconds(), "Total PDU count should be 1 after pruning");
        
        // Check individual PDU type counts
        assertEquals(0, metrics.getDesignatorPdusInLastSixtySeconds(), "Designator PDU count should be 0 after pruning");
        assertEquals(1, metrics.getEntityStatePdusInLastSixtySeconds(), "Entity state PDU count should be 1");
    }
    
    @Test
    void testMultiplePduTypes() {
        // Record different PDU types including designator
        metricsTracker.entityStatePduReceived();
        metricsTracker.designatorPduReceived();
        metricsTracker.fireEventPduReceived();
        
        RealTimeMetrics metrics = metricsTracker.getMetrics();
        
        // Check total PDU count (should be 3)
        assertEquals(3, metrics.getPdusInLastSixtySeconds(), "Total PDU count should be 3");
        
        // Check individual PDU type counts
        assertEquals(1, metrics.getEntityStatePdusInLastSixtySeconds(), "Entity state PDU count should be 1");
        assertEquals(1, metrics.getDesignatorPdusInLastSixtySeconds(), "Designator PDU count should be 1");
        assertEquals(1, metrics.getFireEventPdusInLastSixtySeconds(), "Fire event PDU count should be 1");
    }
}