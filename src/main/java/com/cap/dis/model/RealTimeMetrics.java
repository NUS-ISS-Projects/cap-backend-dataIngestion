package com.cap.dis.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RealTimeMetrics {
    private long lastPduReceivedTimestamp;
    private long pduCountLastProcessingCycle; // Example: PDUs processed since last metrics query or in last minute
    // Add other relevant metrics. For a simple example, we'll stick to these.
}