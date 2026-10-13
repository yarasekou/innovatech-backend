package com.innovatech.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetupSuperAdminDTO {

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, max = 100)
    private String password;

    @Size(max = 100)
    private String prenom;

    @Size(max = 100)
    private String nom;

    @NotBlank(message = "La clé de setup est obligatoire")
    private String secretKey;
}