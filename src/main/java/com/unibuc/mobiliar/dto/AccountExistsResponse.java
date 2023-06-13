package com.unibuc.mobiliar.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountExistsResponse {
    private boolean emailExists;
    private boolean phoneExists;
}
