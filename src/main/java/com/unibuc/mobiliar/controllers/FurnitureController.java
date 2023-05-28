package com.unibuc.mobiliar.controllers;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.unibuc.mobiliar.entities.Furniture;
import com.unibuc.mobiliar.repositories.FurnitureRepository;
import com.unibuc.mobiliar.services.FurnitureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

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

    @PostMapping("/add")
    public ResponseEntity<?> addFurniture(@RequestParam("model") MultipartFile model,
                                          @RequestParam(value = "mtl", required = false) MultipartFile mtl,
                                          @RequestParam(value = "bin", required = false) MultipartFile bin,
                                          @RequestParam(value = "textures", required = false) MultipartFile[] textures,
                                          @RequestParam("name") String name,
                                          @RequestParam("description") String description,
                                          @RequestParam("price") Double price) {
        if (model.isEmpty() || name.isEmpty() || description.isEmpty() || price == null) {
            return new ResponseEntity<>("Please provide all required fields", HttpStatus.BAD_REQUEST);
        }

        String sanitizedFurnitureName = name.replaceAll("[^a-zA-Z0-9-_]", "_");
        String folderName = sanitizedFurnitureName + "-" + UUID.randomUUID().toString();
        String folderUrl = "https://" + cloudFrontDomain + "/models/" + folderName;

        String modelName = uploadFileToS3(model, folderName);
        String modelType = modelName.substring(modelName.lastIndexOf('.') + 1).toLowerCase();

        String materialName = null;
        if (mtl != null) {
            materialName = uploadFileToS3(mtl, folderName);
        }

        String binName = null;
        if (bin != null) {
            binName = uploadFileToS3(bin, folderName);
        }

        Set<String> textureNames = new HashSet<>();
        if (textures != null) {
            for (MultipartFile texture : textures) {
                String textureName = uploadFileToS3(texture, folderName);
                textureNames.add(textureName);
            }
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
            return ResponseEntity.ok(furniture.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Furniture not found with ID: " + id);
        }
    }

    private String uploadFileToS3(MultipartFile file, String folderName) {
        String fileName = file.getOriginalFilename();

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
        Set<String> allowedExtensions = new HashSet<>(Arrays.asList("gltf", "glb", "obj", "mtl", "bin", "png", "jpg", "jpeg"));
        Set<String> allowedContentTypes = new HashSet<>(Arrays.asList(
                "model/gltf+json", // GLTF
                "model/gltf-binary", // GLB
                "application/octet-stream", // OBJ, MTL, BIN
                "image/png", // PNG
                "image/jpeg" // JPG and JPEG
        ));

        return allowedExtensions.contains(fileExtension) && allowedContentTypes.contains(contentType);
    }
}