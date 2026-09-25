package com.innovatech.api.dto;

import com.innovatech.api.entity.StatutCommande;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;   // ⚠️ AJOUTER

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandeResponseDTO {

    private Long id;
    private String numero;
    private ClientSummaryDTO client;
    private StatutCommande statut;
    private String statutLibelle;
    private BigDecimal totalHT;
    private BigDecimal tvaPercent;
    private BigDecimal totalTTC;
    private String notes;
    private LocalDateTime dateCommande;
    private LocalDate dateLivraisonPrevue;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    // ⚠️ AJOUTER CETTE LIGNE
    private List<LigneCommandeResponseDTO> lignes;
}