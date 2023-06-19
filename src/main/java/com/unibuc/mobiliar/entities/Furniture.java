package com.unibuc.mobiliar.entities;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;
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
    private LocalDateTime createdAt;
    @NonNull
    private Double price;
    @NonNull
    private String folderUrl;
    @NonNull
    private String modelType;
    private String modelName;
    private String materialName;
    private String binName;
        private String imageName;
    @ElementCollection
    private Set<String> textureNames;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonBackReference
    private Customer seller;
}