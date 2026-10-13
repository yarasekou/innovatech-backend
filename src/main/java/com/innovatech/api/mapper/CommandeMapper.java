package com.innovatech.api.mapper;

import com.innovatech.api.dto.ClientSummaryDTO;
import com.innovatech.api.dto.CommandeResponseDTO;
import com.innovatech.api.dto.LigneCommandeResponseDTO;
import com.innovatech.api.entity.Commande;
import com.innovatech.api.entity.LigneCommande;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommandeMapper {

    public CommandeResponseDTO toResponseDTO(Commande entity) {
        if (entity == null) return null;

        CommandeResponseDTO.CommandeResponseDTOBuilder builder = CommandeResponseDTO.builder()
                .id(entity.getId())
                .numero(entity.getNumero())
                .client(toClientSummary(entity))
                .statut(entity.getStatut())
                .statutLibelle(entity.getStatut() != null ? entity.getStatut().getLibelle() : null)
                .totalHT(entity.getTotalHT())
                .tvaPercent(entity.getTvaPercent())
                .totalTTC(entity.getTotalTTC())
                .notes(entity.getNotes())
                .dateCommande(entity.getDateCommande())
                .dateLivraisonPrevue(entity.getDateLivraisonPrevue())
                .dateCreation(entity.getDateCreation())
                .dateModification(entity.getDateModification())
                .lignes(toLignesDTO(entity.getLignes()));

        // Entreprise
        if (entity.getEntreprise() != null) {
            builder.entrepriseId(entity.getEntreprise().getId())
                    .entrepriseNom(entity.getEntreprise().getNom());
        }

        // User (boutique)
        if (entity.getUser() != null) {
            builder.userId(entity.getUser().getId())
                    .userEmail(entity.getUser().getEmail())
                    .userNom(entity.getUser().getNom())
                    .userPrenom(entity.getUser().getPrenom());
        }

        return builder.build();
    }

    private ClientSummaryDTO toClientSummary(Commande commande) {
        if (commande.getClient() == null) return null;
        return ClientSummaryDTO.builder()
                .id(commande.getClient().getId())
                .nom(commande.getClient().getNom())
                .prenom(commande.getClient().getPrenom())
                .email(commande.getClient().getEmail())
                .build();
    }

    private LigneCommandeResponseDTO toLigneDTO(LigneCommande ligne) {
        if (ligne == null) return null;

        return LigneCommandeResponseDTO.builder()
                .id(ligne.getId())
                .produitId(ligne.getProduit() != null ? ligne.getProduit().getId() : null)
                .produitReference(ligne.getProduit() != null ? ligne.getProduit().getReference() : null)
                .produitNom(ligne.getProduit() != null ? ligne.getProduit().getNom() : null)
                .quantite(ligne.getQuantite())
                .prixUnitaire(ligne.getPrixUnitaire())
                .sousTotal(ligne.getSousTotal())
                .build();
    }

    public List<LigneCommandeResponseDTO> toLignesDTO(List<LigneCommande> lignes) {
        if (lignes == null || lignes.isEmpty()) return List.of();
        return lignes.stream()
                .map(this::toLigneDTO)
                .toList();
    }
}