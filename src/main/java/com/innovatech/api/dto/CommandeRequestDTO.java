package com.innovatech.api.dto;

import com.innovatech.api.entity.StatutCommande;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO d'entrée pour la création / mise à jour d'une commande.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandeRequestDTO {

    @NotNull(message = "L'identifiant du client est obligatoire")
    private Long clientId;

    @Builder.Default
    private StatutCommande statut = StatutCommande.EN_ATTENTE;

    @DecimalMin(value = "0.00", message = "Le taux de TVA doit être positif ou nul")
    @Digits(integer = 3, fraction = 2, message = "TVA : max 3 chiffres entiers et 2 décimales")
    @Builder.Default
    private BigDecimal tvaPercent = new BigDecimal("18.00");

    @Size(max = 1000, message = "Les notes ne doivent pas dépasser 1000 caractères")
    private String notes;

    private LocalDate dateLivraisonPrevue;

    @NotEmpty(message = "La commande doit contenir au moins une ligne")
    @Valid
    @Builder.Default
    private List<LigneCommandeRequestDTO> lignes = new ArrayList<>();
}