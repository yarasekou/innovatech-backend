package com.innovatech.api.dto;

import com.innovatech.api.entity.StatutRetour;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetourResponseDTO {

    private Long id;
    private String numero;
    private Long commandeId;
    private String commandeNumero;
    private Long clientId;
    private String clientNom;
    private String clientPrenom;
    private String clientEmail;
    private Long userId;
    private String userEmail;
    private StatutRetour statut;
    private String statutLibelle;
    private String motif;
    private String notes;
    private BigDecimal montantTotal;
    private LocalDateTime dateDemande;
    private LocalDateTime dateTraitement;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private List<LigneRetourResponseDTO> lignes;

    // Entreprise
    private Long entrepriseId;
    private String entrepriseNom;
}