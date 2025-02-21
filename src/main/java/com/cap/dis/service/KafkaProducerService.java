package com.cap.dis.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private final String topic = "dis-pdus";

    public void sendMessage(String message) {
        kafkaTemplate.send(topic, message);
        System.out.println("Sent message to Kafka topic '" + topic + "': " + message);
    }
}