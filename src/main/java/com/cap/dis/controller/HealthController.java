package com.cap.dis.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ingestion")
public class HealthController {

    // You can inject other services (like your UDP listener) if needed
    // to perform additional checks.

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> status = new HashMap<>();
        // Here you could check various aspects of your application.
        // For example, check if UDP service is running, Kafka connectivity, etc.
        // For now, we simply return a static status.
        status.put("status", "UP");
        status.put("message", "Data ingestion service is running");
        return status;
    }
}
