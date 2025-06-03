package com.cap.dis.service;

import edu.nps.moves.dis.EntityID;
import edu.nps.moves.dis.EntityStatePdu;
import edu.nps.moves.dis.FirePdu;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
}