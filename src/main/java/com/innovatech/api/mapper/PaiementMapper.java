package com.innovatech.api.mapper;

import com.innovatech.api.dto.PaiementRequestDTO;
import com.innovatech.api.dto.PaiementResponseDTO;
import com.innovatech.api.entity.Commande;
import com.innovatech.api.entity.Entreprise;
import com.innovatech.api.entity.Paiement;
import com.innovatech.api.entity.User;
import org.springframework.stereotype.Component;

@Component
public class PaiementMapper {

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    public Paiement toEntity(PaiementRequestDTO dto,
                             Commande commande,
                             Entreprise entreprise,
                             User user) {
        if (dto == null) return null;

        return Paiement.builder()
                .commande(commande)
                .entreprise(entreprise)
                .user(user)
                .montant(dto.getMontant())
                .methode(dto.getMethode())
                .reference(dto.getReference())
                .notes(dto.getNotes())
                .build();
    }

    // -----------------------------------------
    // RESPONSE
    // -----------------------------------------

    public PaiementResponseDTO toResponseDTO(Paiement entity) {
        if (entity == null) return null;

        PaiementResponseDTO.PaiementResponseDTOBuilder builder = PaiementResponseDTO.builder()
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
                .dateModification(entity.getDateModification());

        if (entity.getEntreprise() != null) {
            builder.entrepriseId(entity.getEntreprise().getId())
                    .entrepriseNom(entity.getEntreprise().getNom());
        }

        if (entity.getUser() != null) {
            builder.userId(entity.getUser().getId())
                    .userEmail(entity.getUser().getEmail())
                    .userNom(entity.getUser().getNom())
                    .userPrenom(entity.getUser().getPrenom());
        }

        return builder.build();
    }
}