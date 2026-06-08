package com.example.invoice.service;

import com.example.invoice.model.Invoice;
import com.example.invoice.repository.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Instant;

@Component
public class KafkaConsumer {

    private final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    private final InvoiceExtractor extractor;
    private final S3Service s3Service;
    private final InvoiceRepository invoiceRepository;

    public KafkaConsumer(InvoiceExtractor extractor, S3Service s3Service, InvoiceRepository invoiceRepository) {
        this.extractor = extractor;
        this.s3Service = s3Service;
        this.invoiceRepository = invoiceRepository;
    }

    @KafkaListener(topics = "${app.kafka.topic:invoice-uploads}", groupId = "${app.kafka.groupId:invoice-consumer-group}")
    public void processInvoice(String message) {
        try {
            // expecting simple JSON like {"bucket":"...","key":"..."}
            String bucket = extractor.extractFieldFromJson(message, "bucket");
            String key = extractor.extractFieldFromJson(message, "key");

            log.info("Processing invoice from S3: bucket={}, key={}", bucket, key);

            File file = s3Service.downloadFile(key);
            Invoice invoice = extractor.extractInvoice(file);
            invoice.setS3Key(key);
            invoice.setCreatedAt(Instant.now());
            invoiceRepository.save(invoice);

            log.info("Invoice processed and saved: {}", invoice.getInvoiceNumber());
        } catch (Exception e) {
            log.error("Failed to process Kafka message: {}", message, e);
        }
    }
}

