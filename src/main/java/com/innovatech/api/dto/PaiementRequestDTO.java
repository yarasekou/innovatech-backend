package com.innovatech.api.dto;

import com.innovatech.api.entity.MethodePaiement;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaiementRequestDTO {

    @NotNull(message = "L'identifiant de la commande est obligatoire")
    private Long commandeId;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    @Digits(integer = 10, fraction = 2, message = "Format du montant invalide")
    private BigDecimal montant;

    @NotNull(message = "La méthode de paiement est obligatoire")
    private MethodePaiement methode;

    @Size(max = 100, message = "La référence ne doit pas dépasser 100 caractères")
    private String reference;

    @Size(max = 500, message = "Les notes ne doivent pas dépasser 500 caractères")
    private String notes;
}