package com.innovatech.api.mapper;

import com.innovatech.api.dto.ClientSummaryDTO;
import com.innovatech.api.dto.CommandeRequestDTO;
import com.innovatech.api.dto.CommandeResponseDTO;
import com.innovatech.api.entity.Client;
import com.innovatech.api.entity.Commande;
import org.springframework.stereotype.Component;

@Component
public class CommandeMapper {

    /**
     * Convertit un DTO de requête en entité.
     * <p>
     * Le client doit être fourni séparément (résolu par le service).
     */
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
                .build();
    }

    private ClientSummaryDTO toClientSummary(Client client) {
        if (client == null) return null;

        return ClientSummaryDTO.builder()
                .id(client.getId())
                .nom(client.getNom())
                .prenom(client.getPrenom())
                .email(client.getEmail())
                .build();
    }
}