package com.cap.dis.service;

import edu.nps.moves.dis.*;
import edu.nps.moves.disutil.PduFactory;
import lombok.RequiredArgsConstructor; // Keep this if you use @RequiredArgsConstructor
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired; // Add if not using constructor injection for all
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

@Service
// If you remove @RequiredArgsConstructor, ensure to add constructor or @Autowired for final fields
public class UdpListenerService {

    private static final Logger log = LoggerFactory.getLogger(UdpListenerService.class); //
    @Value("${udp.port}") 
    private int port; 

    @Value("${udp.buffer-size}") 
    private int bufferSize; 

    private final KafkaProducerService kafkaProducerService; 
    private final PduFactory pduFactory = new PduFactory(); 

    private final DisMetricsTracker metricsTracker; // Autowire or add to constructor

    // Constructor for dependency injection (if not using @RequiredArgsConstructor for all)
    @Autowired
    public UdpListenerService(KafkaProducerService kafkaProducerService, DisMetricsTracker metricsTracker) {
        this.kafkaProducerService = kafkaProducerService;
        this.metricsTracker = metricsTracker;
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

                // Update metrics before attempting to decode or send
                metricsTracker.pduReceived(); 

                String decodedData = decodeDisPdu(rawData); 
                kafkaProducerService.sendMessage(decodedData); 
            }
        } catch (Exception e) {
            log.error("Error in UDP listener", e); 
            System.exit(1); 
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
        StringBuilder json = new StringBuilder("{"); 
        json.append("\"type\":\"").append(pdu.getClass().getSimpleName()).append("\","); 
        json.append("\"protocolVersion\":").append(pdu.getProtocolVersion()).append(","); 
        json.append("\"exerciseID\":").append(pdu.getExerciseID()).append(","); 
        json.append("\"pduType\":").append(pdu.getPduType()).append(","); 
        json.append("\"timestamp\":").append(pdu.getTimestamp()).append(","); 

        if (pdu instanceof EntityStatePdu esp) { 
            json.append("\"entityId\":{"); 
            json.append("\"site\":").append(esp.getEntityID().getSite()).append(","); 
            json.append("\"application\":").append(esp.getEntityID().getApplication()).append(","); 
            json.append("\"entity\":").append(esp.getEntityID().getEntity()); 
            json.append("},"); 
            json.append("\"location\":{"); 
            json.append("\"x\":").append(esp.getEntityLocation().getX()).append(","); 
            json.append("\"y\":").append(esp.getEntityLocation().getY()).append(","); 
            json.append("\"z\":").append(esp.getEntityLocation().getZ()); 
            json.append("}"); 
        } else if (pdu instanceof FirePdu fp) { 
            json.append("\"firingEntityId\":{"); 
            json.append("\"site\":").append(fp.getFiringEntityID().getSite()).append(","); 
            json.append("\"application\":").append(fp.getFiringEntityID().getApplication()).append(","); 
            json.append("\"entity\":").append(fp.getFiringEntityID().getEntity()); 
            json.append("},"); 
            json.append("\"targetEntityId\":{"); 
            json.append("\"site\":").append(fp.getTargetEntityID().getSite()).append(","); 
            json.append("\"application\":").append(fp.getTargetEntityID().getApplication()).append(",");
            json.append("\"entity\":").append(fp.getTargetEntityID().getEntity()); 
            json.append("},");
            json.append("\"munitionId\":{"); 
            json.append("\"site\":").append(fp.getMunitionID().getSite()).append(","); 
            json.append("\"application\":").append(fp.getMunitionID().getApplication()).append(","); 
            json.append("\"entity\":").append(fp.getMunitionID().getEntity()); 
            json.append("}"); 
        } else if (pdu instanceof CollisionPdu cp) { 
            json.append("\"entityId\":{"); 
            json.append("\"site\":").append(cp.getIssuingEntityID().getSite()).append(","); 
            json.append("\"application\":").append(cp.getIssuingEntityID().getApplication()).append(","); 
            json.append("\"entity\":").append(cp.getIssuingEntityID().getEntity()); 
            json.append("},"); 
            json.append("\"collidingEntityId\":{"); 
            json.append("\"site\":").append(cp.getCollidingEntityID().getSite()).append(","); 
            json.append("\"application\":").append(cp.getCollidingEntityID().getApplication()).append(","); 
            json.append("\"entity\":").append(cp.getCollidingEntityID().getEntity()); 
            json.append("}"); 
        } else {
            json.append("\"details\":\"Unhandled PDU type, basic metadata only\""); 
        }

        json.append(",\"processedAt\":").append(System.currentTimeMillis()).append("}"); 
        return json.toString(); 
    }
}
