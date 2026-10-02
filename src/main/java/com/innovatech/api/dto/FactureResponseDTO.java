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

    // Lien
    private Long commandeId;
    private String commandeNumero;

    // Client (actuel)
    private Long clientId;

    // Snapshot client (figé)
    private String clientNom;
    private String clientAdresse;
    private String clientSiret;
    private String clientEmail;
    private String clientTelephone;

    // Montants
    private BigDecimal totalHT;
    private BigDecimal tvaPercent;
    private BigDecimal totalTTC;

    // Dates
    private LocalDate dateEmission;
    private LocalDate dateEcheance;
    private LocalDate datePaiement;

    // Statut
    private StatutFacture statut;
    private String statutLibelle;
    private boolean enRetard;
    private long joursDeRetard;

    // PDF
    private String pdfUrl;

    private String notes;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}