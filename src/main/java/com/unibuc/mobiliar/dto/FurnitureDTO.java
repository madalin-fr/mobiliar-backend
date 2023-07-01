package com.unibuc.mobiliar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FurnitureDTO {
    // Fields from the Furniture entity
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private Double price;
    private String folderUrl;
    private String modelType;
    private String modelName;
    private String materialName;
    private String binName;
    private String imageName;
    private List<String> textureNames;

    // Fields from the Customer entity
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerPassword;
    private String customerAddress1;
    private String customerAddress2;
    private String customerPhoneNumber;
    private boolean customerActivatedAccount;
}