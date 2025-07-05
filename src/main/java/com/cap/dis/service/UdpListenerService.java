package com.cap.dis.service;

import edu.nps.moves.dis.*;
import edu.nps.moves.disutil.PduFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import edu.nps.moves.dis.DataPdu;
import edu.nps.moves.dis.ElectronicEmissionsPdu;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

@Service
public class UdpListenerService {

    private static final Logger log = LoggerFactory.getLogger(UdpListenerService.class);
    
    @Value("${udp.port}")
    private int port;

    @Value("${udp.buffer-size}")
    private int bufferSize;

    private final KafkaProducerService kafkaProducerService;
    private final PduFactory pduFactory = new PduFactory();
    private final DisMetricsTracker metricsTracker;

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

                // Decode PDU first to determine its type
                String decodedData = decodeDisPdu(rawData);
                if (decodedData != null) { // Check if decoding was successful before sending
                    kafkaProducerService.sendMessage(decodedData);
                }
            }
        } catch (Exception e) {
            log.error("Error in UDP listener", e);
            // Consider a more graceful shutdown or retry mechanism instead of System.exit(1) in production
            System.exit(1);
        }
    }

    private String decodeDisPdu(byte[] rawData) {
        try {
            // Check if the data is too small to be a valid PDU
            if (rawData.length < 12) { // Minimum PDU header size is 12 bytes
                log.warn("Received PDU data too small to be valid: {} bytes", rawData.length);
                return "{\"error\":\"PDU data too small to be valid\",\"length\":"+rawData.length+"}";
            }
            
            Pdu pdu = pduFactory.createPdu(rawData);
            if (pdu != null) {
                // Track PDU by type
                if (pdu instanceof EntityStatePdu) {
                    metricsTracker.entityStatePduReceived();
                } else if (pdu instanceof FirePdu) {
                    metricsTracker.fireEventPduReceived();
                } else if (pdu instanceof CollisionPdu) {
                    metricsTracker.collisionPduReceived();
                } else if (pdu instanceof DetonationPdu) {
                    metricsTracker.detonationPduReceived();
                } else if (pdu instanceof DataPdu) {
                    metricsTracker.dataPduReceived();
                } else if (pdu instanceof ActionRequestPdu) {
                    metricsTracker.actionRequestPduReceived();
                } else if (pdu instanceof StartResumePdu) {
                    metricsTracker.startResumePduReceived();
                } else if (pdu instanceof SetDataPdu) {
                    metricsTracker.setDataPduReceived();
                } else if (pdu instanceof DesignatorPdu) {
                    metricsTracker.designatorPduReceived();
                } else if (pdu instanceof ElectronicEmissionsPdu) {
                    metricsTracker.electromagneticEmissionsPduReceived();
                } else {
                    // For other PDU types, just track as a generic PDU
                    metricsTracker.pduReceived();
                }
                
                // This log helps verify what the DIS library returns directly
                log.info("PDU Type: {}, Raw PDU Timestamp from pdu.getTimestamp(): {}",
                         pdu.getClass().getSimpleName(), pdu.getTimestamp());
                return pduToJson(pdu);
            } else {
                log.warn("Received unknown PDU type from raw data of length: {}", rawData.length);
                // It might be useful to log a snippet of the rawData (hex encoded) for unknown types
                return "{\"error\":\"Unknown PDU type\",\"length\":"+rawData.length+"}";
            }
        } catch (java.nio.BufferUnderflowException e) {
            log.error("BufferUnderflowException when decoding PDU: Insufficient data in buffer of length: {}", rawData.length);
            return "{\"error\":\"Insufficient data in PDU buffer\",\"length\":"+rawData.length+"}";
        } catch (Exception e) {
            log.error("Failed to decode PDU from raw data of length: {}", rawData.length, e);
            return "{\"error\":\"Error decoding PDU\",\"length\":"+rawData.length+",\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    private String pduToJson(Pdu pdu) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"type\":\"").append(pdu.getClass().getSimpleName()).append("\",");
        json.append("\"protocolVersion\":").append(pdu.getProtocolVersion()).append(",");
        json.append("\"exerciseID\":").append(pdu.getExerciseID()).append(",");
        json.append("\"pduType\":").append(pdu.getPduType()).append(",");

        // --- TIMESTAMP CORRECTION ---
        long rawPduTimestamp = pdu.getTimestamp();
        // The edu.nps.moves.dis.Pdu.unmarshal uses ByteBuffer.getInt() which reads a signed 32-bit int.
        // If the MSB is set (for absolute DIS time), this results in a negative value.
        // We need to treat it as an unsigned 32-bit value and represent it as a positive long.
        long correctedTimestamp = rawPduTimestamp & 0xFFFFFFFFL;

        log.debug("pduToJson - Raw PDU Timestamp from library: {}, Corrected to unsigned long for JSON: {}", rawPduTimestamp, correctedTimestamp);
        json.append("\"timestamp\":").append(correctedTimestamp).append(",");
        // --- END OF TIMESTAMP CORRECTION ---

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
            json.append("\"issuingEntityId\":{");
            json.append("\"site\":").append(cp.getIssuingEntityID().getSite()).append(",");
            json.append("\"application\":").append(cp.getIssuingEntityID().getApplication()).append(",");
            json.append("\"entity\":").append(cp.getIssuingEntityID().getEntity());
            json.append("},\"collidingEntityId\":{");
            json.append("\"site\":").append(cp.getCollidingEntityID().getSite()).append(",");
            json.append("\"application\":").append(cp.getCollidingEntityID().getApplication()).append(",");
            json.append("\"entity\":").append(cp.getCollidingEntityID().getEntity());
            json.append("}");
        } else if (pdu instanceof DetonationPdu dp) {
            json.append("\"firingEntityId\":{");
            json.append("\"site\":").append(dp.getFiringEntityID().getSite()).append(",");
            json.append("\"application\":").append(dp.getFiringEntityID().getApplication()).append(",");
            json.append("\"entity\":").append(dp.getFiringEntityID().getEntity());
            json.append("},\"targetEntityId\":{");
            json.append("\"site\":").append(dp.getTargetEntityID().getSite()).append(",");
            json.append("\"application\":").append(dp.getTargetEntityID().getApplication()).append(",");
            json.append("\"entity\":").append(dp.getTargetEntityID().getEntity());
            json.append("}");
        } else if (pdu instanceof StartResumePdu srp) {
            json.append("\"realWorldTime\":{");
            json.append("\"hour\":").append(srp.getRealWorldTime().getHour()).append(",");
            json.append("\"timePastHour\":").append(srp.getRealWorldTime().getTimePastHour());
            json.append("}");
        } else if (pdu instanceof ActionRequestPdu arp) {
            json.append("\"originatingEntityId\":{");
            json.append("\"site\":").append(arp.getOriginatingEntityID().getSite()).append(",");
            json.append("\"application\":").append(arp.getOriginatingEntityID().getApplication()).append(",");
            json.append("\"entity\":").append(arp.getOriginatingEntityID().getEntity());
            json.append("},\"receivingEntityId\":{");
            json.append("\"site\":").append(arp.getReceivingEntityID().getSite()).append(",");
            json.append("\"application\":").append(arp.getReceivingEntityID().getApplication()).append(",");
            json.append("\"entity\":").append(arp.getReceivingEntityID().getEntity());
            json.append("}");
        } else if (pdu instanceof SetDataPdu sdp) {
            json.append("\"originatingEntityId\":{");
            json.append("\"site\":").append(sdp.getOriginatingEntityID().getSite()).append(",");
            json.append("\"application\":").append(sdp.getOriginatingEntityID().getApplication()).append(",");
            json.append("\"entity\":").append(sdp.getOriginatingEntityID().getEntity());
            json.append("}");
        } else if (pdu instanceof DesignatorPdu dp) {
            json.append("\"designatingEntityId\":{");
            json.append("\"site\":").append(dp.getDesignatingEntityID().getSite()).append(",");
            json.append("\"application\":").append(dp.getDesignatingEntityID().getApplication()).append(",");
            json.append("\"entity\":").append(dp.getDesignatingEntityID().getEntity());
            json.append("},");
            json.append("\"designatedEntityId\":{");
            json.append("\"site\":").append(dp.getDesignatedEntityID().getSite()).append(",");
            json.append("\"application\":").append(dp.getDesignatedEntityID().getApplication()).append(",");
            json.append("\"entity\":").append(dp.getDesignatedEntityID().getEntity());
            json.append("}");
        } else if (pdu instanceof DataPdu dataPdu) {
            json.append("\"originatingEntityId\":{");
            json.append("\"site\":").append(dataPdu.getOriginatingEntityID().getSite()).append(",");
            json.append("\"application\":").append(dataPdu.getOriginatingEntityID().getApplication()).append(",");
            json.append("\"entity\":").append(dataPdu.getOriginatingEntityID().getEntity());
            json.append("},\"receivingEntityId\":{");
            json.append("\"site\":").append(dataPdu.getReceivingEntityID().getSite()).append(",");
            json.append("\"application\":").append(dataPdu.getReceivingEntityID().getApplication()).append(",");
            json.append("\"entity\":").append(dataPdu.getReceivingEntityID().getEntity());
            json.append("}");
        } else if (pdu instanceof ElectronicEmissionsPdu eep) {
            json.append("\"emittingEntityId\":{");
            json.append("\"site\":").append(eep.getEmittingEntityID().getSite()).append(",");
            json.append("\"application\":").append(eep.getEmittingEntityID().getApplication()).append(",");
            json.append("\"entity\":").append(eep.getEmittingEntityID().getEntity());
            json.append("}");
        } else {
            // For other PDU types, we might still want basic info
            json.append("\"details\":\"Unhandled PDU type for detailed JSON structure, basic metadata only\"");
        }

        json.append(",\"processedAt\":").append(System.currentTimeMillis()).append("}");
        return json.toString();
    }
}