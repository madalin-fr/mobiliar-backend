package com.unibuc.mobiliar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FurnitureUpdateDTO {
    private String name;
    private String description;
    private Double price;
}
