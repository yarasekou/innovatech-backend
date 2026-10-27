package com.innovatech.api.dto;

import com.innovatech.api.entity.MethodePaiement;
import com.innovatech.api.entity.StatutPaiement;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaiementResponseDTO {

    private Long id;
    private String numero;

    private Long commandeId;
    private String commandeNumero;

    // ⚠️ NOUVEAU
    private Long commandeAchatId;
    private String commandeAchatNumero;

    private BigDecimal montant;
    private MethodePaiement methode;
    private String methodeLibelle;
    private StatutPaiement statut;
    private String statutLibelle;
    private String reference;
    private String notes;
    private LocalDateTime datePaiement;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    private Long entrepriseId;
    private String entrepriseNom;

    private Long userId;
    private String userEmail;
    private String userNom;
    private String userPrenom;
}