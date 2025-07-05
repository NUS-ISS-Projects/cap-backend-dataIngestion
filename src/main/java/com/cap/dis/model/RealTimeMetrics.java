package com.cap.dis.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RealTimeMetrics {
    private long lastPduReceivedTimestampMs; // Timestamp of the absolute latest PDU in milliseconds
    private long pdusInLastSixtySeconds;     // Count of PDUs received in the strictly last 60 seconds
    private double averagePduRatePerSecondLastSixtySeconds; // Calculated: pdusInLastSixtySeconds / 60.0
    
    // Counts for specific PDU types
    private long entityStatePdusInLastSixtySeconds;
    private long fireEventPdusInLastSixtySeconds;
    private long collisionPdusInLastSixtySeconds;
    private long detonationPdusInLastSixtySeconds;
    private long dataPdusInLastSixtySeconds;
    private long actionRequestPdusInLastSixtySeconds;
    private long startResumePdusInLastSixtySeconds;
    private long setDataPdusInLastSixtySeconds;
    private long designatorPdusInLastSixtySeconds;
    private long electromagneticEmissionsPdusInLastSixtySeconds;
    
    // Constructor for backward compatibility
    public RealTimeMetrics(long lastPduReceivedTimestampMs, long pdusInLastSixtySeconds, 
                          double averagePduRatePerSecondLastSixtySeconds) {
        this.lastPduReceivedTimestampMs = lastPduReceivedTimestampMs;
        this.pdusInLastSixtySeconds = pdusInLastSixtySeconds;
        this.averagePduRatePerSecondLastSixtySeconds = averagePduRatePerSecondLastSixtySeconds;
        this.entityStatePdusInLastSixtySeconds = 0;
        this.fireEventPdusInLastSixtySeconds = 0;
        this.collisionPdusInLastSixtySeconds = 0;
        this.detonationPdusInLastSixtySeconds = 0;
        this.dataPdusInLastSixtySeconds = 0;
        this.actionRequestPdusInLastSixtySeconds = 0;
        this.startResumePdusInLastSixtySeconds = 0;
        this.setDataPdusInLastSixtySeconds = 0;
        this.designatorPdusInLastSixtySeconds = 0;
        this.electromagneticEmissionsPdusInLastSixtySeconds = 0;
    }
    
    // Constructor for original four PDU types
    public RealTimeMetrics(long lastPduReceivedTimestampMs, long pdusInLastSixtySeconds, 
                          double averagePduRatePerSecondLastSixtySeconds,
                          long entityStatePdusInLastSixtySeconds,
                          long fireEventPdusInLastSixtySeconds,
                          long collisionPdusInLastSixtySeconds,
                          long detonationPdusInLastSixtySeconds) {
        this.lastPduReceivedTimestampMs = lastPduReceivedTimestampMs;
        this.pdusInLastSixtySeconds = pdusInLastSixtySeconds;
        this.averagePduRatePerSecondLastSixtySeconds = averagePduRatePerSecondLastSixtySeconds;
        this.entityStatePdusInLastSixtySeconds = entityStatePdusInLastSixtySeconds;
        this.fireEventPdusInLastSixtySeconds = fireEventPdusInLastSixtySeconds;
        this.collisionPdusInLastSixtySeconds = collisionPdusInLastSixtySeconds;
        this.detonationPdusInLastSixtySeconds = detonationPdusInLastSixtySeconds;
        this.dataPdusInLastSixtySeconds = 0;
        this.actionRequestPdusInLastSixtySeconds = 0;
        this.startResumePdusInLastSixtySeconds = 0;
        this.setDataPdusInLastSixtySeconds = 0;
        this.designatorPdusInLastSixtySeconds = 0;
        this.electromagneticEmissionsPdusInLastSixtySeconds = 0;
    }
    
    // Full constructor with all PDU types
    public RealTimeMetrics(long lastPduReceivedTimestampMs, long pdusInLastSixtySeconds, 
                          double averagePduRatePerSecondLastSixtySeconds,
                          long entityStatePdusInLastSixtySeconds,
                          long fireEventPdusInLastSixtySeconds,
                          long collisionPdusInLastSixtySeconds,
                          long detonationPdusInLastSixtySeconds,
                          long dataPdusInLastSixtySeconds,
                          long actionRequestPdusInLastSixtySeconds,
                          long startResumePdusInLastSixtySeconds,
                          long setDataPdusInLastSixtySeconds,
                          long designatorPdusInLastSixtySeconds,
                          long electromagneticEmissionsPdusInLastSixtySeconds) {
        this.lastPduReceivedTimestampMs = lastPduReceivedTimestampMs;
        this.pdusInLastSixtySeconds = pdusInLastSixtySeconds;
        this.averagePduRatePerSecondLastSixtySeconds = averagePduRatePerSecondLastSixtySeconds;
        this.entityStatePdusInLastSixtySeconds = entityStatePdusInLastSixtySeconds;
        this.fireEventPdusInLastSixtySeconds = fireEventPdusInLastSixtySeconds;
        this.collisionPdusInLastSixtySeconds = collisionPdusInLastSixtySeconds;
        this.detonationPdusInLastSixtySeconds = detonationPdusInLastSixtySeconds;
        this.dataPdusInLastSixtySeconds = dataPdusInLastSixtySeconds;
        this.actionRequestPdusInLastSixtySeconds = actionRequestPdusInLastSixtySeconds;
        this.startResumePdusInLastSixtySeconds = startResumePdusInLastSixtySeconds;
        this.setDataPdusInLastSixtySeconds = setDataPdusInLastSixtySeconds;
        this.designatorPdusInLastSixtySeconds = designatorPdusInLastSixtySeconds;
        this.electromagneticEmissionsPdusInLastSixtySeconds = electromagneticEmissionsPdusInLastSixtySeconds;
    }
}