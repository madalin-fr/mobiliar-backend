package com.unibuc.mobiliar.controllers;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.util.IOUtils;
import com.unibuc.mobiliar.dto.FurnitureDTO;
import com.unibuc.mobiliar.entities.Customer;
import com.unibuc.mobiliar.entities.Furniture;
import com.unibuc.mobiliar.services.AWSService;
import com.unibuc.mobiliar.services.CustomerService;
import com.unibuc.mobiliar.services.FurnitureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/furniture")
public class FurnitureController {

    private final FurnitureService furnitureService;

    private final AWSService awsService;

    private final CustomerService customerService;

    private static final Logger logger = LoggerFactory.getLogger(FurnitureController.class);
    public FurnitureController(AmazonS3 s3Client, FurnitureService furnitureService, AWSService awsService, CustomerService customerService) {
        this.furnitureService = furnitureService;
        this.awsService = awsService;
        this.customerService = customerService;
    }
    @PostMapping("/add")
    public ResponseEntity<?> addFurniture(@RequestParam("model") MultipartFile model,
                                          @RequestParam(value = "mtl", required = false) MultipartFile mtl,
                                          @RequestParam(value = "textures", required = false) MultipartFile[] textures,
                                          @RequestParam("name") String name,
                                          @RequestParam("description") String description,
                                          @RequestParam("price") Double price,
                                          @RequestParam(value = "image", required = false) MultipartFile image,
                                          @RequestParam("customerEmail") String customerEmail){
        logger.info("Adding new furniture with name: {}", name);
        if (model.isEmpty() || name.isEmpty() || description.isEmpty() || price == null) {
            return new ResponseEntity<>("Please provide all required fields", HttpStatus.BAD_REQUEST);
        }
        String sanitizedFurnitureName = name.replaceAll("[^a-zA-Z0-9-_]", "_");
        String folderName = sanitizedFurnitureName + "-" + UUID.randomUUID();
        String folderUrl = "https://" + awsService.getCloudFrontDomain() + "/models/" + folderName;
        String modelName = awsService.uploadFileToS3(model, folderName);
        String modelType = null;
        if (modelName != null) {
            modelType = modelName.substring(modelName.lastIndexOf('.') + 1).toLowerCase();
        }
        String materialName = null;
        if (mtl != null) {
            materialName = awsService.uploadFileToS3(mtl, folderName);
        }
        Set<String> textureNames = new HashSet<>();
        if (textures !=  null) {
            for (MultipartFile texture : textures) {
                String textureName = awsService.uploadFileToS3(texture, folderName);
                textureNames.add(textureName);
            }
        }

        String imageName = null;
        String newImageName = "environment";
        if (image != null) {
            imageName = awsService.uploadFileToS3(image, folderName, newImageName);
        }

        if (modelType == null) {
            throw new IllegalArgumentException("modelType cannot be null");
        }

        Optional<Customer> seller = customerService.findCustomerByEmail(customerEmail);
        if (seller == null) {
            return new ResponseEntity<>("Customer not found", HttpStatus.BAD_REQUEST);
        }

        Furniture furniture = Furniture.builder()
                .name(name)
                .description(description)
                .price(price)
                .createdAt(LocalDateTime.now())
                .folderUrl(folderUrl)
                .modelType(modelType)
                .modelName(modelName)
                .materialName(materialName)
                .textureNames(textureNames)
                .imageName(imageName)
                .seller(seller.get())
                .build();
        furnitureService.saveFurniture(furniture);
        logger.info("Furniture added successfully with id: {}", furniture.getId());
        return ResponseEntity.ok().body(furniture);
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> getFurnitureById(@PathVariable Long id) {
        logger.info("Fetching furniture with id: {}", id);
        Optional<Furniture> furniture = furnitureService.getFurnitureById(id);
        if (furniture.isPresent()) {
            Furniture furnitureItem = furniture.get();
            Customer customer = furnitureItem.getSeller();
            FurnitureDTO furnitureDTO = new FurnitureDTO(
                    furnitureItem.getId(),
                    furnitureItem.getName(),
                    furnitureItem.getDescription(),
                    furnitureItem.getCreatedAt(),
                    furnitureItem.getPrice(),
                    furnitureItem.getFolderUrl(),
                    furnitureItem.getModelType(),
                    furnitureItem.getModelName(),
                    furnitureItem.getMaterialName(),
                    furnitureItem.getBinName(),
                    furnitureItem.getImageName(),
                    furnitureItem.getTextureNames(),
                    customer.getId(),
                    customer.getName(),
                    customer.getEmail(),
                    customer.getPassword(),
                    customer.getAddress1(),
                    customer.getAddress2(),
                    customer.getPhoneNumber(),
                    customer.isActivatedAccount()
            );
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS)) // Set Cache-Control header to cache for 7 days
                    .body(furnitureDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Furniture not found with ID: " + id);
        }
    }

    @GetMapping
    public ResponseEntity<List<FurnitureDTO>> getAllFurniture() {
        logger.info("Fetching all furniture");
        List<Furniture> allFurniture = furnitureService.getAllFurniture();
        List<FurnitureDTO> allFurnitureDto = allFurniture.stream()
                .map(furniture -> {
                    Customer customer = furniture.getSeller();
                    return new FurnitureDTO(
                            furniture.getId(),
                            furniture.getName(),
                            furniture.getDescription(),
                            furniture.getCreatedAt(),
                            furniture.getPrice(),
                            furniture.getFolderUrl(),
                            furniture.getModelType(),
                            furniture.getModelName(),
                            furniture.getMaterialName(),
                            furniture.getBinName(),
                            furniture.getImageName(),
                            furniture.getTextureNames(),
                            customer.getId(),
                            customer.getName(),
                            customer.getEmail(),
                            customer.getPassword(),
                            customer.getAddress1(),
                            customer.getAddress2(),
                            customer.getPhoneNumber(),
                            customer.isActivatedAccount()
                    );
                })
                .collect(Collectors.toList());
        logger.info("Returning {} furniture items", allFurniture.size());
        return ResponseEntity.ok(allFurnitureDto);
    }

    @GetMapping("/{id}/{fileName}")
    public ResponseEntity<?> getFurnitureFile(@PathVariable Long id, @PathVariable String fileName)
    {
        logger.info("Fetching file '{}' for furniture with id: {}", fileName, id);
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

    @DeleteMapping("/{id}")
    public ResponseEntity<?> removeFurniture(@PathVariable Long id, @RequestParam("customerEmail") String customerEmail) {
        logger.info("Removing furniture with id: {}", id);
        Optional<Furniture> furnitureOptional = furnitureService.getFurnitureById(id);
        if (furnitureOptional.isPresent()) {
            Furniture furniture = furnitureOptional.get();
            if (!furniture.getSeller().getEmail().equals(customerEmail)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not the owner of this furniture item");
            }
            // Delete files associated with the furniture (model, texture, image)
            awsService.deleteFileFromS3(furniture.getModelName(), furniture.getFolderUrl());
            if (furniture.getMaterialName() != null) {
                awsService.deleteFileFromS3(furniture.getMaterialName(), furniture.getFolderUrl());
            }
            for (String textureName : furniture.getTextureNames()) {
                awsService.deleteFileFromS3(textureName, furniture.getFolderUrl());
            }
            if (furniture.getImageName() != null) {
                awsService.deleteFileFromS3(furniture.getImageName(), furniture.getFolderUrl());
            }

            // Delete furniture from the database
            furnitureService.removeFurniture(furniture);
            logger.info("Furniture removed successfully");
            return ResponseEntity.ok().body("Furniture removed successfully");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Furniture not found with ID: " + id);
        }
    }


}