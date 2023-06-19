package com.unibuc.mobiliar.entities;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.*;
import javax.persistence.*;
import java.util.List;
import java.util.Set;

@Entity
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor(force = true)
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NonNull
    private String name;
    @NonNull
    private String email;
    @NonNull
    private String password;
    @NonNull
    private String address1;
    private String address2;
    @NonNull
    private String phoneNumber;
    private boolean activatedAccount = false;


    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Furniture> furnitures;


}
