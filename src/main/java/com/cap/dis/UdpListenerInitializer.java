package com.cap.dis;

import com.cap.dis.service.UdpListenerService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class UdpListenerInitializer implements CommandLineRunner {

    private final UdpListenerService udpListenerService;

    public UdpListenerInitializer(UdpListenerService udpListenerService) {
        this.udpListenerService = udpListenerService;
    }

    @Override
    public void run(String... args) throws Exception {
        udpListenerService.startListening();
    }
}
