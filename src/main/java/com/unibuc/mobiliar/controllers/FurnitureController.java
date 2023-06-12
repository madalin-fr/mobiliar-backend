package com.unibuc.mobiliar.controllers;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.util.IOUtils;
import com.unibuc.mobiliar.entities.Furniture;
import com.unibuc.mobiliar.services.FurnitureService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/furniture")
public class FurnitureController {

    @Autowired
    private AmazonS3 s3Client;

    @Autowired
    private FurnitureService furnitureService;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.cloudfront.domain}")
    private String cloudFrontDomain;


    private static final Logger logger = LogManager.getLogger(FurnitureController.class);

    @PostMapping("/add")
    public ResponseEntity<?> addFurniture(@RequestParam("model") MultipartFile model,
                                          @RequestParam(value = "mtl", required = false) MultipartFile mtl,
                                          @RequestParam(value = "textures", required = false) MultipartFile[] textures,
                                          @RequestParam("name") String name,
                                          @RequestParam("description") String description,
                                          @RequestParam("price") Double price) {
        if (model.isEmpty() || name.isEmpty() || description.isEmpty() || price == null) {
            return new ResponseEntity<>("Please provide all required fields", HttpStatus.BAD_REQUEST);
        }

        String sanitizedFurnitureName = name.replaceAll("[^a-zA-Z0-9-_]", "_");
        String folderName = sanitizedFurnitureName + "-" + UUID.randomUUID();
        String folderUrl = "https://" + cloudFrontDomain + "/models/" + folderName;

        String modelName = uploadFileToS3(model, folderName);
        String modelType = null;
        if (modelName != null) {
            modelType = modelName.substring(modelName.lastIndexOf('.') + 1).toLowerCase();
        }
        String materialName = null;
        if (mtl != null) {
            materialName = uploadFileToS3(mtl, folderName);
        }


        Set<String> textureNames = new HashSet<>();
        if (textures !=  null) {
            for (MultipartFile texture : textures) {
                String textureName = uploadFileToS3(texture, folderName);
                textureNames.add(textureName);
            }
        }

        if (modelType == null) {
            throw new IllegalArgumentException("modelType cannot be null");
        }

        Furniture furniture = Furniture.builder()
                .name(name)
                .description(description)
                .price(price)
                .folderUrl(folderUrl)
                .modelType(modelType)
                .modelName(modelName)
                .materialName(materialName)
                .textureNames(textureNames)
                .build();


        furnitureService.saveFurniture(furniture);

        return ResponseEntity.ok().body(furniture);
    }

    @GetMapping
    public ResponseEntity<List<Furniture>> getAllFurniture() {
        return ResponseEntity.ok(furnitureService.getAllFurniture());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFurnitureById(@PathVariable Long id) {
        Optional<Furniture> furniture = furnitureService.getFurnitureById(id);
        if (furniture.isPresent()) {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS)) // Set Cache-Control header to cache for 7 days
                    .body(furniture.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Furniture not found with ID: " + id);
        }
    }

    private String uploadFileToS3(MultipartFile file, String folderName) {
        String fileName = file.getOriginalFilename();

        if (fileName == null) {
            return null;
        }

        String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        String contentType = file.getContentType();

        if (!isFileValid(fileExtension, contentType)) {
            return null;
        }

        String s3Key = "models/" + folderName + "/" + fileName;

        try {
            File tempFile = File.createTempFile("temp", null);
            file.transferTo(tempFile);

            // Upload the object
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, s3Key, tempFile);
            s3Client.putObject(putObjectRequest);

            return fileName;

        } catch (IOException e) {
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


    @GetMapping("/{id}/{fileName}")
        public ResponseEntity<?> getFurnitureFile(@PathVariable Long id, @PathVariable String fileName) {
        Optional<Furniture> furniture = furnitureService.getFurnitureById(id);
        if (furniture.isPresent()) {
            String cloudFrontUrl = furniture.get().getFolderUrl() + "/" + fileName;
            try {
                URL url = new URL(cloudFrontUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                InputStream objectData = connection.getInputStream();
                byte[] bytes = IOUtils.toByteArray(objectData);

                return ResponseEntity.ok().contentType(MediaType.parseMediaType(connection.getContentType())).body(bytes);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving file from S3: " + e.getMessage());
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Furniture not found with ID: " + id);
        }
    }
}