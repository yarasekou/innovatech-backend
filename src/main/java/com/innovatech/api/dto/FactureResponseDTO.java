package com.innovatech.api.dto;

import com.innovatech.api.entity.StatutFacture;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FactureResponseDTO {

    private Long id;
    private String numero;

    private Long commandeId;
    private String commandeNumero;

    private Long clientId;
    private String clientNom;
    private String clientAdresse;
    private String clientSiret;
    private String clientEmail;
    private String clientTelephone;

    private BigDecimal totalHT;
    private BigDecimal tvaPercent;
    private BigDecimal totalTTC;

    private LocalDate dateEmission;
    private LocalDate dateEcheance;
    private LocalDate datePaiement;

    private StatutFacture statut;
    private String statutLibelle;
    private boolean enRetard;
    private long joursDeRetard;

    private String pdfUrl;
    private String notes;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    // Entreprise
    private Long entrepriseId;
    private String entrepriseNom;

    // User (boutique)
    private Long userId;
    private String userEmail;
    private String userNom;
    private String userPrenom;
}