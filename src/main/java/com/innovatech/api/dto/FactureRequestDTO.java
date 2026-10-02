package com.innovatech.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * DTO pour créer une facture à partir d'une commande.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FactureRequestDTO {

    @NotNull(message = "L'identifiant de la commande est obligatoire")
    private Long commandeId;

    /**
     * Date d'émission (optionnelle, par défaut : aujourd'hui).
     */
    private LocalDate dateEmission;

    @Size(max = 1000, message = "Les notes ne doivent pas dépasser 1000 caractères")
    private String notes;
}