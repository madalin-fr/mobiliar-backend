package com.unibuc.mobiliar.entities;
import lombok.*;
import javax.persistence.*;
import java.util.Set;
@Entity
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor(force = true)
public class Furniture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NonNull
    private String name;
    @NonNull
    private String description;
    @NonNull
    private Double price;
    @NonNull
    private String folderUrl;
    @NonNull
    private String modelType;
    private String modelName;
    private String materialName;
    private String binName;
    @ElementCollection
    private Set<String> textureNames;
}