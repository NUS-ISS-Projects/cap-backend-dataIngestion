package com.cap.dis.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private KafkaProducerService kafkaProducerService;

    private final String testTopic = "test-dis-pdus";
    private final String testMessage = "{\"type\":\"TestPdu\", \"data\":\"test_data\"}";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(kafkaProducerService, "topic", testTopic);
    }

    @Test
    void sendMessage_success() {
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(eq(testTopic), anyString())).thenReturn(future);

        kafkaProducerService.sendMessage(testMessage);

        verify(kafkaTemplate, times(1)).send(testTopic, testMessage); // [cite: 176]
        // Add assertions for logging if a Captor is used for logger
    }

    @Test
    void sendMessage_failure() {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka send error"));
        when(kafkaTemplate.send(eq(testTopic), anyString())).thenReturn(future);

        kafkaProducerService.sendMessage(testMessage);

        verify(kafkaTemplate, times(1)).send(testTopic, testMessage); // [cite: 176]
        // Add assertions for error logging if a Captor is used for logger [cite: 177]
    }
}