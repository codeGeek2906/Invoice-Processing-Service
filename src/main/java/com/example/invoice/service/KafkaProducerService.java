package com.example.invoice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.topic:invoice-uploads}")
    private String invoiceTopic;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishInvoiceUploadedEvent(String bucket, String key) {
        String message = "{\"bucket\":\"" + bucket + "\",\"key\":\"" + key + "\"}";
        kafkaTemplate.send(invoiceTopic, message);
        log.info("Published invoice event to topic {}: {}", invoiceTopic, message);
    }
}

