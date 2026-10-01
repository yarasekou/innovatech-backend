package com.innovatech.api.dto;

import com.innovatech.api.entity.StatutAchat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandeAchatResponseDTO {

    private Long id;
    private String numero;
    private FournisseurSummaryDTO fournisseur;
    private StatutAchat statut;
    private String statutLibelle;
    private BigDecimal totalHT;
    private BigDecimal tvaPercent;
    private BigDecimal totalTTC;
    private String notes;
    private LocalDateTime dateCommande;
    private LocalDate dateReceptionPrevue;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private List<LigneCommandeAchatResponseDTO> lignes;

    // Acheteur
    private Long acheteurId;
    private String acheteurEmail;
    private String acheteurNom;
    private String acheteurPrenom;
}