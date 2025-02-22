package com.cap.dis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.cap.dis.service.UdpListenerService;

@SpringBootApplication
public class DataIngestionApplication implements CommandLineRunner {

    @Autowired
    private UdpListenerService udpListenerService;

    public static void main(String[] args) {
        SpringApplication.run(DataIngestionApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Start the UDP listener in a separate thread
        new Thread(() -> {
            udpListenerService.startListening();
        }).start();
    }
}