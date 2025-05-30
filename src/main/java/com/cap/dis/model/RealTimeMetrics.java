package com.cap.dis.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RealTimeMetrics {
    private long lastPduReceivedTimestampMs; // Timestamp of the absolute latest PDU in milliseconds
    private long pdusInLastSixtySeconds;     // Count of PDUs received in the strictly last 60 seconds
    private double averagePduRatePerSecondLastSixtySeconds; // Calculated: pdusInLastSixtySeconds / 60.0
}