package com.innovatech.api.mapper;

import com.innovatech.api.dto.PaiementRequestDTO;
import com.innovatech.api.dto.PaiementResponseDTO;
import com.innovatech.api.entity.*;
import org.springframework.stereotype.Component;

@Component
public class PaiementMapper {

    // -----------------------------------------
    // CREATE (Pour une vente)
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
    // CREATE (Pour un achat) ⚠️ NOUVEAU
    // -----------------------------------------
    public Paiement toEntityAchat(PaiementRequestDTO dto,
                                  CommandeAchat commandeAchat,
                                  Entreprise entreprise,
                                  User user) {
        if (dto == null) return null;

        return Paiement.builder()
                .commandeAchat(commandeAchat)
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

        // Pour une vente
        if (entity.getCommande() != null) {
            builder.commandeId(entity.getCommande().getId())
                    .commandeNumero(entity.getCommande().getNumero());
        }

        // ⚠️ NOUVEAU : Pour un achat
        if (entity.getCommandeAchat() != null) {
            builder.commandeAchatId(entity.getCommandeAchat().getId())
                    .commandeAchatNumero(entity.getCommandeAchat().getNumero());
        }

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