package com.example.invoice.controller;

import com.example.invoice.service.KafkaProducerService;
import com.example.invoice.service.S3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/invoices")
public class InvoiceController {

    private final S3Service s3Service;
    private final KafkaProducerService kafkaProducerService;

    public InvoiceController(S3Service s3Service, KafkaProducerService kafkaProducerService) {
        this.s3Service = s3Service;
        this.kafkaProducerService = kafkaProducerService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadInvoice(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("file is empty");
        }

        String s3Key = s3Service.uploadFile(file.getOriginalFilename(), file.getInputStream(), file.getContentType());
        // publish a Kafka event with the S3 location
        kafkaProducerService.publishInvoiceUploadedEvent(s3Service.getBucketName(), s3Key);

        return ResponseEntity.accepted().body("uploaded: " + s3Key);
    }
}

