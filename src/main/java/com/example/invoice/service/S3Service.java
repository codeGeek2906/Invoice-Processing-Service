package com.example.invoice.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${app.s3.bucket:invoice-uploads}")
    private String bucketName;

    public S3Service(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String uploadFile(String originalFilename, InputStream data, String contentType) throws IOException {
        String key = UUID.randomUUID().toString() + "_" + (originalFilename == null ? "file" : originalFilename);
        ObjectMetadata metadata = new ObjectMetadata();
        // content-length unknown here; leaving unset
        if (contentType != null) metadata.setContentType(contentType);
        amazonS3.putObject(bucketName, key, data, metadata);
        return key;
    }

    public File downloadFile(String key) throws IOException {
        File tmp = File.createTempFile("invoice_", "");
        try (FileOutputStream fos = new FileOutputStream(tmp)) {
            amazonS3.getObject(bucketName, key).getObjectContent().transferTo(fos);
        }
        return tmp;
    }
}

