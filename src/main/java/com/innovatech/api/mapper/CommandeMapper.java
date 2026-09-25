package com.innovatech.api.mapper;

import com.innovatech.api.dto.ClientSummaryDTO;
import com.innovatech.api.dto.CommandeRequestDTO;
import com.innovatech.api.dto.CommandeResponseDTO;
import com.innovatech.api.dto.LigneCommandeResponseDTO;
import com.innovatech.api.entity.Client;
import com.innovatech.api.entity.Commande;
import com.innovatech.api.entity.LigneCommande;
import org.springframework.stereotype.Component;
import com.innovatech.api.dto.LigneCommandeResponseDTO;
import com.innovatech.api.entity.LigneCommande;
import java.util.List;
import java.util.List;

@Component
public class CommandeMapper {

    public Commande toEntity(CommandeRequestDTO dto, Client client) {
        if (dto == null) return null;

        return Commande.builder()
                .client(client)
                .statut(dto.getStatut() != null ? dto.getStatut() : null)
                .tvaPercent(dto.getTvaPercent() != null ? dto.getTvaPercent() : null)
                .notes(dto.getNotes())
                .dateLivraisonPrevue(dto.getDateLivraisonPrevue())
                .build();
    }

    public void updateEntity(Commande entity, CommandeRequestDTO dto, Client client) {
        entity.setClient(client);
        entity.setTvaPercent(dto.getTvaPercent() != null ? dto.getTvaPercent() : entity.getTvaPercent());
        entity.setNotes(dto.getNotes());
        entity.setDateLivraisonPrevue(dto.getDateLivraisonPrevue());
    }

    /**
     * Convertit une entité Commande en DTO de réponse.
     * <p>
     * ⚠️ Inclut désormais la liste des lignes.
     */
    public CommandeResponseDTO toResponseDTO(Commande entity) {
        if (entity == null) return null;

        return CommandeResponseDTO.builder()
                .id(entity.getId())
                .numero(entity.getNumero())
                .client(toClientSummary(entity.getClient()))
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
                .lignes(toLignesDTO(entity.getLignes()))
                .build();
    }

    // -----------------------------------------
    // Méthodes privées / utilitaires
    // -----------------------------------------

    private ClientSummaryDTO toClientSummary(Client client) {
        if (client == null) return null;

        return ClientSummaryDTO.builder()
                .id(client.getId())
                .nom(client.getNom())
                .prenom(client.getPrenom())
                .email(client.getEmail())
                .build();
    }

    /**
     * Convertit une LigneCommande en DTO de réponse.
     */
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

    /**
     * Convertit une liste de LigneCommande en liste de DTOs.
     */
    public List<LigneCommandeResponseDTO> toLignesDTO(List<LigneCommande> lignes) {
        if (lignes == null || lignes.isEmpty()) return List.of();

        return lignes.stream()
                .map(this::toLigneDTO)
                .toList();
    }
}