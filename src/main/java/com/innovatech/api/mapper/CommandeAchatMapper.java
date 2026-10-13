package com.innovatech.api.mapper;

import com.innovatech.api.dto.*;
import com.innovatech.api.entity.CommandeAchat;
import com.innovatech.api.entity.Entreprise;
import com.innovatech.api.entity.Fournisseur;
import com.innovatech.api.entity.LigneCommandeAchat;
import com.innovatech.api.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommandeAchatMapper {

    public CommandeAchat toEntity(CommandeAchatRequestDTO dto,
                                  Entreprise entreprise,
                                  User user,
                                  Fournisseur fournisseur) {
        if (dto == null) return null;

        return CommandeAchat.builder()
                .entreprise(entreprise)
                .user(user)
                .fournisseur(fournisseur)
                .tvaPercent(dto.getTvaPercent() != null
                        ? dto.getTvaPercent()
                        : new java.math.BigDecimal("18.00"))
                .notes(dto.getNotes())
                .dateReceptionPrevue(dto.getDateReceptionPrevue())
                .build();
    }

    public void updateEntity(CommandeAchat entity,
                             CommandeAchatRequestDTO dto,
                             Fournisseur fournisseur) {
        entity.setFournisseur(fournisseur);
        entity.setTvaPercent(dto.getTvaPercent() != null
                ? dto.getTvaPercent()
                : entity.getTvaPercent());
        entity.setNotes(dto.getNotes());
        entity.setDateReceptionPrevue(dto.getDateReceptionPrevue());
    }

    public CommandeAchatResponseDTO toResponseDTO(CommandeAchat entity) {
        if (entity == null) return null;

        CommandeAchatResponseDTO.CommandeAchatResponseDTOBuilder builder =
                CommandeAchatResponseDTO.builder()
                        .id(entity.getId())
                        .numero(entity.getNumero())
                        .fournisseur(toFournisseurSummary(entity.getFournisseur()))
                        .statut(entity.getStatut())
                        .statutLibelle(entity.getStatut() != null ? entity.getStatut().getLibelle() : null)
                        .totalHT(entity.getTotalHT())
                        .tvaPercent(entity.getTvaPercent())
                        .totalTTC(entity.getTotalTTC())
                        .notes(entity.getNotes())
                        .dateCommande(entity.getDateCommande())
                        .dateReceptionPrevue(entity.getDateReceptionPrevue())
                        .dateCreation(entity.getDateCreation())
                        .dateModification(entity.getDateModification())
                        .lignes(toLignesDTO(entity.getLignes()));

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

    private FournisseurSummaryDTO toFournisseurSummary(Fournisseur f) {
        if (f == null) return null;
        return FournisseurSummaryDTO.builder()
                .id(f.getId())
                .code(f.getCode())
                .nom(f.getNom())
                .email(f.getEmail())
                .build();
    }

    private LigneCommandeAchatResponseDTO toLigneDTO(LigneCommandeAchat ligne) {
        if (ligne == null) return null;
        return LigneCommandeAchatResponseDTO.builder()
                .id(ligne.getId())
                .produitId(ligne.getProduit() != null ? ligne.getProduit().getId() : null)
                .produitReference(ligne.getProduit() != null ? ligne.getProduit().getReference() : null)
                .produitNom(ligne.getProduit() != null ? ligne.getProduit().getNom() : null)
                .quantite(ligne.getQuantite())
                .prixAchatUnitaire(ligne.getPrixAchatUnitaire())
                .sousTotal(ligne.getSousTotal())
                .build();
    }

    public List<LigneCommandeAchatResponseDTO> toLignesDTO(List<LigneCommandeAchat> lignes) {
        if (lignes == null || lignes.isEmpty()) return List.of();
        return lignes.stream().map(this::toLigneDTO).toList();
    }
}