package com.cap.dis.service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UdpListenerService {

    private static final int PORT = 3000;
    private static final int BUFFER_SIZE = 2048;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    public void startListening() {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            System.out.println("UDP Listener started on port " + PORT);
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String rawData = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                
                // Simulate decoding the DIS PDU
                String decodedData = decodeDisPdu(rawData);
                
                // Publish the decoded data to Kafka
                kafkaProducerService.sendMessage(decodedData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String decodeDisPdu(String rawData) {
        // TODO: Replace with real decoding logic for DIS PDUs
        return "Decoded PDU: " + rawData;
    }
}