package com.innovatech.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetourRequestDTO {

    @NotNull(message = "L'identifiant de la commande est obligatoire")
    private Long commandeId;

    @NotBlank(message = "Le motif est obligatoire")
    @Size(max = 500, message = "Le motif ne doit pas dépasser 500 caractères")
    private String motif;

    @Size(max = 1000, message = "Les notes ne doivent pas dépasser 1000 caractères")
    private String notes;

    @NotEmpty(message = "Le retour doit contenir au moins une ligne")
    @Valid
    @Builder.Default
    private List<LigneRetourRequestDTO> lignes = new ArrayList<>();
}