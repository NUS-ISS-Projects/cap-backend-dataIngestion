package com.cap.dis.service;

import edu.nps.moves.dis.EntityID;
import edu.nps.moves.dis.EntityStatePdu;
import edu.nps.moves.dis.FirePdu;
import edu.nps.moves.dis.CollisionPdu;
import edu.nps.moves.dis.DetonationPdu;
import edu.nps.moves.dis.Pdu;
import edu.nps.moves.disutil.PduFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

// Import for Reflection
import java.lang.reflect.Method;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;


// ... (imports and other methods remain the same)

@ExtendWith(MockitoExtension.class)
class UdpListenerServiceTest {

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private DisMetricsTracker metricsTracker; // Mock is still needed if other tests use it or if startListening was testable here

    @Spy
    private PduFactory pduFactory = new PduFactory();

    @InjectMocks
    private UdpListenerService udpListenerService;

    private ObjectMapper objectMapper = new ObjectMapper();

    // Helper method to invoke private methods via reflection
    private Object invokePrivateMethod(Object object, String methodName, Class<?>[] parameterTypes, Object[] parameters) throws Exception {
        Method method = object.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(object, parameters);
    }

    private EntityStatePdu createSampleEntityStatePdu() {
        EntityStatePdu espdu = new EntityStatePdu();
        espdu.setProtocolVersion((short) 7);
        espdu.setExerciseID((short) 1);
        espdu.setPduType((short) 1);
        espdu.setTimestamp(1234567890L);

        EntityID entityID = new EntityID();
        entityID.setSite(18);
        entityID.setApplication(23);
        entityID.setEntity(1001);
        espdu.setEntityID(entityID);

        espdu.getEntityLocation().setX(10.0);
        espdu.getEntityLocation().setY(20.0);
        espdu.getEntityLocation().setZ(30.0);
        return espdu;
    }
    
    private FirePdu createSampleFirePdu() {
        FirePdu fpdu = new FirePdu();
        fpdu.setProtocolVersion((short) 7);
        fpdu.setExerciseID((short) 1);
        fpdu.setPduType((short) 2);
        fpdu.setTimestamp(1234567891L);

        EntityID firingEntityID = new EntityID();
        firingEntityID.setSite(18);
        firingEntityID.setApplication(23);
        firingEntityID.setEntity(1002);
        fpdu.setFiringEntityID(firingEntityID);

        EntityID targetEntityID = new EntityID();
        targetEntityID.setSite(18);
        targetEntityID.setApplication(23);
        targetEntityID.setEntity(1003);
        fpdu.setTargetEntityID(targetEntityID);
        
        EntityID munitionID = new EntityID();
        munitionID.setSite(18);
        munitionID.setApplication(23);
        munitionID.setEntity(50);
        fpdu.setMunitionID(munitionID);
        
        return fpdu;
    }

    // ... (pduToJson tests remain the same as they don't involve metricsTracker)
    @Test
    void testPduToJson_entityStatePdu() throws Exception {
        EntityStatePdu pdu = createSampleEntityStatePdu();
        long expectedJsonTimestamp = pdu.getTimestamp() & 0xFFFFFFFFL;

        String jsonResult = (String) invokePrivateMethod(
            udpListenerService,
            "pduToJson",
            new Class<?>[]{Pdu.class},
            new Object[]{pdu}
        );
        JsonNode rootNode = objectMapper.readTree(jsonResult);

        assertEquals("EntityStatePdu", rootNode.get("type").asText());
        assertEquals(7, rootNode.get("protocolVersion").asInt());
        assertEquals(1, rootNode.get("exerciseID").asInt());
        assertEquals(1, rootNode.get("pduType").asInt());
        assertEquals(expectedJsonTimestamp, rootNode.get("timestamp").asLong());
        assertEquals(18, rootNode.get("entityId").get("site").asInt());
        assertEquals(23, rootNode.get("entityId").get("application").asInt());
        assertEquals(1001, rootNode.get("entityId").get("entity").asInt());
        assertEquals(10.0, rootNode.get("location").get("x").asDouble());
        assertNotNull(rootNode.get("processedAt"));
    }
    
    @Test
    void testPduToJson_firePdu() throws Exception {
        FirePdu pdu = createSampleFirePdu();
        long expectedJsonTimestamp = pdu.getTimestamp() & 0xFFFFFFFFL;

        String jsonResult = (String) invokePrivateMethod(
            udpListenerService,
            "pduToJson",
            new Class<?>[]{Pdu.class},
            new Object[]{pdu}
        );
        JsonNode rootNode = objectMapper.readTree(jsonResult);

        assertEquals("FirePdu", rootNode.get("type").asText());
        assertEquals(expectedJsonTimestamp, rootNode.get("timestamp").asLong());
        assertEquals(18, rootNode.get("firingEntityId").get("site").asInt());
        assertEquals(23, rootNode.get("firingEntityId").get("application").asInt());
        assertEquals(1002, rootNode.get("firingEntityId").get("entity").asInt());
        assertEquals(18, rootNode.get("targetEntityId").get("site").asInt());
        assertEquals(50, rootNode.get("munitionId").get("entity").asInt());
        assertNotNull(rootNode.get("processedAt"));
    }

    @Test
    void testPduToJson_unhandledPduType() throws Exception {
        Pdu genericPdu = new Pdu(); 
        genericPdu.setProtocolVersion((short) 7);
        genericPdu.setExerciseID((short) 1);
        genericPdu.setPduType((short) 99); 
        genericPdu.setTimestamp(1234567892L);
        long expectedJsonTimestamp = genericPdu.getTimestamp() & 0xFFFFFFFFL;

        String jsonResult = (String) invokePrivateMethod(
            udpListenerService,
            "pduToJson",
            new Class<?>[]{Pdu.class},
            new Object[]{genericPdu}
        );
        JsonNode rootNode = objectMapper.readTree(jsonResult);

        assertEquals("Pdu", rootNode.get("type").asText()); 
        assertEquals(expectedJsonTimestamp, rootNode.get("timestamp").asLong());
        assertEquals("Unhandled PDU type for detailed JSON structure, basic metadata only", rootNode.get("details").asText());
        assertNotNull(rootNode.get("processedAt"));
    }
    
    @Test
    void testDecodeDisPdu_bufferUnderflowException() throws Exception {
        // Create a byte array that's large enough to pass the size check but will cause BufferUnderflowException
        byte[] data = new byte[15]; // More than minimum PDU size (12 bytes)
        
        // Mock the PduFactory to throw BufferUnderflowException
        doThrow(BufferUnderflowException.class).when(pduFactory).createPdu(any(ByteBuffer.class));
        
        // Call the method
        String result = (String) invokePrivateMethod(
            udpListenerService,
            "decodeDisPdu",
            new Class<?>[]{byte[].class},
            new Object[]{data}
        );
        
        // Debug output to see what's actually in the result
        System.out.println("BufferUnderflowException test result: " + result);
        
        // Verify the result contains the error message with data length
        JsonNode rootNode = objectMapper.readTree(result);
        assertTrue(rootNode.has("error"), "Error field is missing in the response");
        String errorText = rootNode.get("error").asText();
        System.out.println("Error text: " + errorText);
        
        // Check if the error message contains expected text
        assertTrue(errorText.contains("Error decoding PDU") || 
                   errorText.contains("Insufficient data") || 
                   errorText.contains("BufferUnderflowException"), 
                   "Error message doesn't contain expected text: " + errorText);
        assertTrue(rootNode.has("length"), "Length field is missing in the response");
    }
    
    @Test
    void testDecodeDisPdu_dataTooSmall() throws Exception {
        // Create a byte array that's too small to be a valid PDU header
        byte[] tooSmallData = new byte[10]; // Less than minimum PDU size (12 bytes)
        
        // Call the method
        String result = (String) invokePrivateMethod(
            udpListenerService,
            "decodeDisPdu",
            new Class<?>[]{byte[].class},
            new Object[]{tooSmallData}
        );
        
        // Verify the result contains the error message with data length
        JsonNode rootNode = objectMapper.readTree(result);
        assertTrue(rootNode.has("error"));
        assertEquals("PDU data too small to be valid", rootNode.get("error").asText());
        assertEquals(10, rootNode.get("length").asInt());
    }
    
    @Test
    void testDecodeDisPdu_unknownPduType() throws Exception {
        // Mock the PduFactory to return null (unknown PDU type)
        when(pduFactory.createPdu(any(ByteBuffer.class))).thenReturn(null);
        
        // Create a byte array that's large enough to be a valid PDU
        byte[] validSizeData = new byte[20];
        
        // Call the method
        String result = (String) invokePrivateMethod(
            udpListenerService,
            "decodeDisPdu",
            new Class<?>[]{byte[].class},
            new Object[]{validSizeData}
        );
        
        // Verify the result contains the error message
        JsonNode rootNode = objectMapper.readTree(result);
        assertTrue(rootNode.has("error"));
        assertEquals("Unknown PDU type", rootNode.get("error").asText());
    }
    
    @Test
    void testDecodeDisPdu_entityStatePdu() throws Exception {
        // Create a sample EntityStatePdu
        EntityStatePdu pdu = createSampleEntityStatePdu();
        
        // Mock the PduFactory to return the EntityStatePdu
        when(pduFactory.createPdu(any(ByteBuffer.class))).thenReturn(pdu);
        
        // Create a byte array that's large enough to be a valid PDU
        byte[] validSizeData = new byte[20];
        
        // Call the method
        String result = (String) invokePrivateMethod(
            udpListenerService,
            "decodeDisPdu",
            new Class<?>[]{byte[].class},
            new Object[]{validSizeData}
        );
        
        // Verify that entityStatePduReceived was called
        verify(metricsTracker, times(1)).entityStatePduReceived();
        verify(metricsTracker, never()).fireEventPduReceived();
        verify(metricsTracker, never()).collisionPduReceived();
        verify(metricsTracker, never()).detonationPduReceived();
        verify(metricsTracker, never()).pduReceived();
        
        // Verify the result is a valid JSON
        JsonNode rootNode = objectMapper.readTree(result);
        assertEquals("EntityStatePdu", rootNode.get("type").asText());
    }
    
    @Test
    void testDecodeDisPdu_firePdu() throws Exception {
        // Create a sample FirePdu
        FirePdu pdu = createSampleFirePdu();
        
        // Mock the PduFactory to return the FirePdu
        when(pduFactory.createPdu(any(ByteBuffer.class))).thenReturn(pdu);
        
        // Create a byte array that's large enough to be a valid PDU
        byte[] validSizeData = new byte[20];
        
        // Call the method
        String result = (String) invokePrivateMethod(
            udpListenerService,
            "decodeDisPdu",
            new Class<?>[]{byte[].class},
            new Object[]{validSizeData}
        );
        
        // Verify that fireEventPduReceived was called
        verify(metricsTracker, never()).entityStatePduReceived();
        verify(metricsTracker, times(1)).fireEventPduReceived();
        verify(metricsTracker, never()).collisionPduReceived();
        verify(metricsTracker, never()).detonationPduReceived();
        verify(metricsTracker, never()).pduReceived();
        
        // Verify the result is a valid JSON
        JsonNode rootNode = objectMapper.readTree(result);
        assertEquals("FirePdu", rootNode.get("type").asText());
    }
    
    private CollisionPdu createSampleCollisionPdu() {
        CollisionPdu cpdu = new CollisionPdu();
        cpdu.setProtocolVersion((short) 7);
        cpdu.setExerciseID((short) 1);
        cpdu.setPduType((short) 4);
        cpdu.setTimestamp(1234567892L);

        EntityID issuingEntityID = new EntityID();
        issuingEntityID.setSite(18);
        issuingEntityID.setApplication(23);
        issuingEntityID.setEntity(1004);
        cpdu.setIssuingEntityID(issuingEntityID);

        EntityID collidingEntityID = new EntityID();
        collidingEntityID.setSite(18);
        collidingEntityID.setApplication(23);
        collidingEntityID.setEntity(1005);
        cpdu.setCollidingEntityID(collidingEntityID);
        
        return cpdu;
    }
    
    private DetonationPdu createSampleDetonationPdu() {
        DetonationPdu dpdu = new DetonationPdu();
        dpdu.setProtocolVersion((short) 7);
        dpdu.setExerciseID((short) 1);
        dpdu.setPduType((short) 3);
        dpdu.setTimestamp(1234567893L);

        EntityID firingEntityID = new EntityID();
        firingEntityID.setSite(18);
        firingEntityID.setApplication(23);
        firingEntityID.setEntity(1006);
        dpdu.setFiringEntityID(firingEntityID);

        EntityID targetEntityID = new EntityID();
        targetEntityID.setSite(18);
        targetEntityID.setApplication(23);
        targetEntityID.setEntity(1007);
        dpdu.setTargetEntityID(targetEntityID);
        
        return dpdu;
    }
    
    @Test
    void testDecodeDisPdu_collisionPdu() throws Exception {
        // Create a sample CollisionPdu
        CollisionPdu pdu = createSampleCollisionPdu();
        
        // Mock the PduFactory to return the CollisionPdu
        when(pduFactory.createPdu(any(ByteBuffer.class))).thenReturn(pdu);
        
        // Create a byte array that's large enough to be a valid PDU
        byte[] validSizeData = new byte[20];
        
        // Call the method
        String result = (String) invokePrivateMethod(
            udpListenerService,
            "decodeDisPdu",
            new Class<?>[]{byte[].class},
            new Object[]{validSizeData}
        );
        
        // Verify that collisionPduReceived was called
        verify(metricsTracker, never()).entityStatePduReceived();
        verify(metricsTracker, never()).fireEventPduReceived();
        verify(metricsTracker, times(1)).collisionPduReceived();
        verify(metricsTracker, never()).detonationPduReceived();
        verify(metricsTracker, never()).pduReceived();
        
        // Verify the result is a valid JSON
        JsonNode rootNode = objectMapper.readTree(result);
        assertEquals("CollisionPdu", rootNode.get("type").asText());
    }
    
    @Test
    void testDecodeDisPdu_detonationPdu() throws Exception {
        // Create a sample DetonationPdu
        DetonationPdu pdu = createSampleDetonationPdu();
        
        // Mock the PduFactory to return the DetonationPdu
        when(pduFactory.createPdu(any(ByteBuffer.class))).thenReturn(pdu);
        
        // Create a byte array that's large enough to be a valid PDU
        byte[] validSizeData = new byte[20];
        
        // Call the method
        String result = (String) invokePrivateMethod(
            udpListenerService,
            "decodeDisPdu",
            new Class<?>[]{byte[].class},
            new Object[]{validSizeData}
        );
        
        // Verify that detonationPduReceived was called
        verify(metricsTracker, never()).entityStatePduReceived();
        verify(metricsTracker, never()).fireEventPduReceived();
        verify(metricsTracker, never()).collisionPduReceived();
        verify(metricsTracker, times(1)).detonationPduReceived();
        verify(metricsTracker, never()).pduReceived();
        
        // Verify the result is a valid JSON
        JsonNode rootNode = objectMapper.readTree(result);
        assertEquals("DetonationPdu", rootNode.get("type").asText());
    }
    
    @Test
    void testDecodeDisPdu_otherPduType() throws Exception {
        // Create a generic Pdu that doesn't match any of the specific types
        Pdu pdu = new Pdu();
        pdu.setProtocolVersion((short) 7);
        pdu.setExerciseID((short) 1);
        pdu.setPduType((short) 99);
        pdu.setTimestamp(1234567894L);
        
        // Mock the PduFactory to return the generic Pdu
        when(pduFactory.createPdu(any(ByteBuffer.class))).thenReturn(pdu);
        
        // Create a byte array that's large enough to be a valid PDU
        byte[] validSizeData = new byte[20];
        
        // Call the method
        String result = (String) invokePrivateMethod(
            udpListenerService,
            "decodeDisPdu",
            new Class<?>[]{byte[].class},
            new Object[]{validSizeData}
        );
        
        // Verify that the generic pduReceived was called
        verify(metricsTracker, never()).entityStatePduReceived();
        verify(metricsTracker, never()).fireEventPduReceived();
        verify(metricsTracker, never()).collisionPduReceived();
        verify(metricsTracker, never()).detonationPduReceived();
        verify(metricsTracker, times(1)).pduReceived();
        
        // Verify the result is a valid JSON
        JsonNode rootNode = objectMapper.readTree(result);
        assertEquals("Pdu", rootNode.get("type").asText());
    }
}