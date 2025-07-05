package com.cap.dis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.nps.moves.dis.DesignatorPdu;
import edu.nps.moves.dis.EntityID;
import edu.nps.moves.dis.Pdu;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UdpListenerServiceDesignatorTest {

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private DisMetricsTracker metricsTracker;

    @InjectMocks
    private UdpListenerService udpListenerService;

    private ObjectMapper objectMapper = new ObjectMapper();

    // Helper method to invoke private methods via reflection
    private Object invokePrivateMethod(Object object, String methodName, Class<?>[] parameterTypes, Object[] parameters) throws Exception {
        Method method = object.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(object, parameters);
    }

    private DesignatorPdu createSampleDesignatorPdu() {
        DesignatorPdu dpdu = new DesignatorPdu();
        dpdu.setProtocolVersion((short) 7);
        dpdu.setExerciseID((short) 1);
        dpdu.setPduType((short) 24); // Designator PDU type
        dpdu.setTimestamp(1234567895L);

        EntityID designatingEntityID = new EntityID();
        designatingEntityID.setSite(18);
        designatingEntityID.setApplication(23);
        designatingEntityID.setEntity(1008);
        dpdu.setDesignatingEntityID(designatingEntityID);

        EntityID designatedEntityID = new EntityID();
        designatedEntityID.setSite(18);
        designatedEntityID.setApplication(23);
        designatedEntityID.setEntity(1009);
        dpdu.setDesignatedEntityID(designatedEntityID);
        
        return dpdu;
    }

    @Test
    void testPduToJson_designatorPdu() throws Exception {
        // Arrange
        DesignatorPdu pdu = createSampleDesignatorPdu();
        long expectedJsonTimestamp = pdu.getTimestamp() & 0xFFFFFFFFL;

        // Act
        String jsonResult = (String) invokePrivateMethod(
            udpListenerService,
            "pduToJson",
            new Class<?>[]{Pdu.class},
            new Object[]{pdu}
        );
        
        // Assert
        JsonNode rootNode = objectMapper.readTree(jsonResult);

        assertEquals("DesignatorPdu", rootNode.get("type").asText());
        assertEquals(7, rootNode.get("protocolVersion").asInt());
        assertEquals(1, rootNode.get("exerciseID").asInt());
        assertEquals(24, rootNode.get("pduType").asInt());
        assertEquals(expectedJsonTimestamp, rootNode.get("timestamp").asLong());
        
        // Check designatingEntityId
        assertNotNull(rootNode.get("designatingEntityId"));
        assertEquals(18, rootNode.get("designatingEntityId").get("site").asInt());
        assertEquals(23, rootNode.get("designatingEntityId").get("application").asInt());
        assertEquals(1008, rootNode.get("designatingEntityId").get("entity").asInt());
        
        // Check designatedEntityId
        assertNotNull(rootNode.get("designatedEntityId"));
        assertEquals(18, rootNode.get("designatedEntityId").get("site").asInt());
        assertEquals(23, rootNode.get("designatedEntityId").get("application").asInt());
        assertEquals(1009, rootNode.get("designatedEntityId").get("entity").asInt());
        
        assertNotNull(rootNode.get("processedAt"));
    }

    @Test
    void testDecodeDisPdu_designatorPdu() throws Exception {
        // Arrange
        DesignatorPdu pdu = createSampleDesignatorPdu();
        
        // We need to use reflection to set the pduFactory field
        java.lang.reflect.Field pduFactoryField = UdpListenerService.class.getDeclaredField("pduFactory");
        pduFactoryField.setAccessible(true);
        
        // Get the current pduFactory
        edu.nps.moves.disutil.PduFactory originalPduFactory = (edu.nps.moves.disutil.PduFactory) pduFactoryField.get(udpListenerService);
        
        // Create a spy of the original pduFactory
        edu.nps.moves.disutil.PduFactory spyPduFactory = spy(originalPduFactory);
        
        // Mock the createPdu method to return our DesignatorPdu
        // Specify the byte[] version of createPdu to avoid ambiguity
        doReturn(pdu).when(spyPduFactory).createPdu(any(byte[].class));
        
        // Set the spy pduFactory back to the service
        pduFactoryField.set(udpListenerService, spyPduFactory);
        
        // Create a byte array that's large enough to be a valid PDU
        byte[] validSizeData = new byte[20];
        
        // Act
        String result = (String) invokePrivateMethod(
            udpListenerService,
            "decodeDisPdu",
            new Class<?>[]{byte[].class},
            new Object[]{validSizeData}
        );
        
        // Assert
        // Verify that designatorPduReceived was called
        verify(metricsTracker, times(1)).designatorPduReceived();
        verify(metricsTracker, never()).entityStatePduReceived();
        verify(metricsTracker, never()).fireEventPduReceived();
        verify(metricsTracker, never()).collisionPduReceived();
        verify(metricsTracker, never()).detonationPduReceived();
        verify(metricsTracker, never()).pduReceived();
        
        // Verify the result is a valid JSON
        JsonNode rootNode = objectMapper.readTree(result);
        assertEquals("DesignatorPdu", rootNode.get("type").asText());
        
        // Reset the original pduFactory
        pduFactoryField.set(udpListenerService, originalPduFactory);
    }
}