package com.cap.dis.service;

import edu.nps.moves.dis.EntityStatePdu;
import edu.nps.moves.dis.Pdu;
import edu.nps.moves.disutil.PduFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class UdpListenerService {

    private static final Logger log = LoggerFactory.getLogger(UdpListenerService.class);

    @Value("${udp.port}")
    private int port;

    @Value("${udp.buffer-size}")
    private int bufferSize;

    private final KafkaProducerService kafkaProducerService;

    private final PduFactory pduFactory = new PduFactory();

    @PostConstruct
    public void init() {
        startListening();
    }

    @Async
    public void startListening() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] buffer = new byte[bufferSize];
            log.info("UDP Listener started on port {}", port);
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                byte[] rawData = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
                String decodedData = decodeDisPdu(rawData);
                kafkaProducerService.sendMessage(decodedData);
            }
        } catch (BindException e) {
            log.error("Failed to bind to UDP port {}: Address already in use. Please free the port or configure a different one in application.properties.", port, e);
            // Optionally, shut down the application
            System.exit(1); // Exit with error code
        } catch (Exception e) {
            log.error("Error in UDP listener", e);
        }
    }

    private String decodeDisPdu(byte[] rawData) {
        try {
            Pdu pdu = pduFactory.createPdu(rawData);
            if (pdu != null) {
                return pduToJson(pdu);
            } else {
                log.warn("Received unknown PDU type");
                return "{\"error\":\"Unknown PDU type\"}";
            }
        } catch (Exception e) {
            log.error("Failed to decode PDU", e);
            return "{\"error\":\"Error decoding PDU\"}";
        }
    }

    private String pduToJson(Pdu pdu) {
        if (pdu instanceof EntityStatePdu esp) {
            return String.format(
                "{\"type\":\"EntityStatePdu\"," +
                "\"entityId\":{\"site\":%d,\"application\":%d,\"entity\":%d}," +
                "\"location\":{\"x\":%.2f,\"y\":%.2f,\"z\":%.2f}," +
                "\"timestamp\":%d}",
                esp.getEntityID().getSite(),
                esp.getEntityID().getApplication(),
                esp.getEntityID().getEntity(),
                esp.getEntityLocation().getX(),
                esp.getEntityLocation().getY(),
                esp.getEntityLocation().getZ(),
                System.currentTimeMillis()
            );
        }
        return "{\"type\":\"" + pdu.getClass().getSimpleName() + "\", \"timestamp\":" + System.currentTimeMillis() + "}";
    }
}