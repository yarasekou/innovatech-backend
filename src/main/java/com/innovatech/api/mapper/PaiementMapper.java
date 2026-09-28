package com.innovatech.api.mapper;

import com.innovatech.api.dto.PaiementRequestDTO;
import com.innovatech.api.dto.PaiementResponseDTO;
import com.innovatech.api.entity.Commande;
import com.innovatech.api.entity.Paiement;
import org.springframework.stereotype.Component;

@Component
public class PaiementMapper {

    public Paiement toEntity(PaiementRequestDTO dto, Commande commande) {
        if (dto == null) return null;

        return Paiement.builder()
                .commande(commande)
                .montant(dto.getMontant())
                .methode(dto.getMethode())
                .reference(dto.getReference())
                .notes(dto.getNotes())
                .build();
    }

    public PaiementResponseDTO toResponseDTO(Paiement entity) {
        if (entity == null) return null;

        return PaiementResponseDTO.builder()
                .id(entity.getId())
                .numero(entity.getNumero())
                .commandeId(entity.getCommande() != null ? entity.getCommande().getId() : null)
                .commandeNumero(entity.getCommande() != null ? entity.getCommande().getNumero() : null)
                .montant(entity.getMontant())
                .methode(entity.getMethode())
                .methodeLibelle(entity.getMethode() != null ? entity.getMethode().getLibelle() : null)
                .statut(entity.getStatut())
                .statutLibelle(entity.getStatut() != null ? entity.getStatut().getLibelle() : null)
                .reference(entity.getReference())
                .notes(entity.getNotes())
                .datePaiement(entity.getDatePaiement())
                .dateCreation(entity.getDateCreation())
                .dateModification(entity.getDateModification())
                .build();
    }
}