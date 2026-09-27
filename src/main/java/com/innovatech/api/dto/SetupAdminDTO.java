package com.innovatech.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetupAdminDTO {

    @NotBlank(message = "La clé de setup est obligatoire")
    private String secretKey;
}