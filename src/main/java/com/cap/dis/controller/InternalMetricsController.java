package com.cap.dis.controller;

import com.cap.dis.model.RealTimeMetrics;
import com.cap.dis.service.DisMetricsTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/metrics") // Internal endpoint, not exposed via main Ingress typically
public class InternalMetricsController {

    private final DisMetricsTracker metricsTracker;

    @Autowired
    public InternalMetricsController(DisMetricsTracker metricsTracker) {
        this.metricsTracker = metricsTracker;
    }

    @GetMapping("/realtime")
    public ResponseEntity<RealTimeMetrics> getRealTimeDisMetrics() {
        return ResponseEntity.ok(metricsTracker.getMetrics());
    }
}