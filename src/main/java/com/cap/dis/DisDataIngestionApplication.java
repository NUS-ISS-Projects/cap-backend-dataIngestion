package com.cap.dis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DisDataIngestionApplication {
    public static void main(String[] args) {
        SpringApplication.run(DisDataIngestionApplication.class, args);
    }
}