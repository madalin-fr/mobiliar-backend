package com.unibuc.mobiliar.services;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.unibuc.mobiliar.controllers.FurnitureController;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class AWSService {
    private final AmazonS3 s3Client;
    @Getter
    @Value("${aws.s3.bucket}")
    private String bucketName;
    @Getter
    @Value("${aws.cloudfront.domain}")
    private String cloudFrontDomain;

    private static final Logger logger = LoggerFactory.getLogger(AWSService.class);

    public AWSService(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadFileToS3(MultipartFile file, String folderName) {
        return uploadFileToS3(file, folderName, null);
    }

    public String uploadFileToS3(MultipartFile file, String folderName, String newFileName) {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            return null;
        }
        String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        String contentType = file.getContentType();
        if (!isFileValid(fileExtension, contentType)) {
            return null;
        }
        if (newFileName != null) {
            fileName = newFileName + "." + fileExtension;
        }
        String s3Key = "models/" + folderName + "/" + fileName;
        try {
            File tempFile = File.createTempFile("temp", null);
            file.transferTo(tempFile);
            // Upload the object
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, s3Key, tempFile);
            s3Client.putObject(putObjectRequest);
            logger.info("File '{}' uploaded to S3 successfully", fileName);
            return fileName;
        } catch (IOException e) {
            logger.error("Error uploading file '{}' to S3: {}", fileName, e.getMessage(), e);
            return null;
        }
    }
    private boolean isFileValid(String fileExtension, String contentType) {
        Set<String> allowedExtensions = new HashSet<>(Arrays.asList("obj", "mtl", "png", "jpg", "jpeg"));
        Set<String> allowedContentTypes = new HashSet<>(Arrays.asList(
                "application/octet-stream", // OBJ, MTL
                "image/png", // PNG
                "image/jpeg" // JPG and JPEG
        ));
        return allowedExtensions.contains(fileExtension) && allowedContentTypes.contains(contentType);
    }

    public void deleteFileFromS3(String modelName, String folderUrl) {
        String s3Key = "models/" + folderUrl + "/" + modelName;
        try {
            s3Client.deleteObject(bucketName, s3Key);
            logger.info("File '{}' deleted from S3 successfully", modelName);
        } catch (Exception e) {
            logger.error("Error deleting file '{}' from S3: {}", modelName, e.getMessage(), e);
        }
    }
}
