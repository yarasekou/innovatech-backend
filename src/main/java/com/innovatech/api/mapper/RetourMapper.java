package com.innovatech.api.mapper;

import com.innovatech.api.dto.*;
import com.innovatech.api.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RetourMapper {

    public Retour toEntity(RetourRequestDTO dto, Commande commande, Client client, User user) {
        if (dto == null) return null;

        return Retour.builder()
                .commande(commande)
                .client(client)
                .user(user)
                .motif(dto.getMotif())
                .notes(dto.getNotes())
                .statut(StatutRetour.DEMANDE)
                .build();
    }

    public RetourResponseDTO toResponseDTO(Retour entity) {
        if (entity == null) return null;

        RetourResponseDTO.RetourResponseDTOBuilder builder = RetourResponseDTO.builder()
                .id(entity.getId())
                .numero(entity.getNumero())
                .statut(entity.getStatut())
                .statutLibelle(entity.getStatut() != null ? entity.getStatut().getLibelle() : null)
                .motif(entity.getMotif())
                .notes(entity.getNotes())
                .montantTotal(entity.getMontantTotal())
                .dateDemande(entity.getDateDemande())
                .dateTraitement(entity.getDateTraitement())
                .dateCreation(entity.getDateCreation())
                .dateModification(entity.getDateModification())
                .lignes(toLignesDTO(entity.getLignes()));

        if (entity.getCommande() != null) {
            builder.commandeId(entity.getCommande().getId())
                    .commandeNumero(entity.getCommande().getNumero());
        }

        if (entity.getClient() != null) {
            builder.clientId(entity.getClient().getId())
                    .clientNom(entity.getClient().getNom())
                    .clientPrenom(entity.getClient().getPrenom())
                    .clientEmail(entity.getClient().getEmail());
        }

        if (entity.getUser() != null) {
            builder.userId(entity.getUser().getId())
                    .userEmail(entity.getUser().getEmail());
        }

        return builder.build();
    }

    private LigneRetourResponseDTO toLigneDTO(LigneRetour ligne) {
        if (ligne == null) return null;

        return LigneRetourResponseDTO.builder()
                .id(ligne.getId())
                .produitId(ligne.getProduit() != null ? ligne.getProduit().getId() : null)
                .produitReference(ligne.getProduit() != null ? ligne.getProduit().getReference() : null)
                .produitNom(ligne.getProduit() != null ? ligne.getProduit().getNom() : null)
                .quantite(ligne.getQuantite())
                .prixUnitaire(ligne.getPrixUnitaire())
                .sousTotal(ligne.getSousTotal())
                .raison(ligne.getRaison())
                .build();
    }

    public List<LigneRetourResponseDTO> toLignesDTO(List<LigneRetour> lignes) {
        if (lignes == null || lignes.isEmpty()) return List.of();
        return lignes.stream().map(this::toLigneDTO).toList();
    }
}