package com.innovatech.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandeAchatRequestDTO {

    @NotNull(message = "L'identifiant du fournisseur est obligatoire")
    private Long fournisseurId;

    @DecimalMin(value = "0.00", message = "Le taux de TVA doit être positif ou nul")
    @Digits(integer = 3, fraction = 2, message = "TVA : max 3 chiffres entiers et 2 décimales")
    @Builder.Default
    private BigDecimal tvaPercent = new BigDecimal("18.00");

    @Size(max = 1000, message = "Les notes ne doivent pas dépasser 1000 caractères")
    private String notes;

    private LocalDate dateReceptionPrevue;

    @NotEmpty(message = "La commande doit contenir au moins une ligne")
    @Valid
    @Builder.Default
    private List<LigneCommandeAchatRequestDTO> lignes = new ArrayList<>();
}