package com.innovatech.api.mapper;

import com.innovatech.api.dto.ClientRequestDTO;
import com.innovatech.api.dto.ClientResponseDTO;
import com.innovatech.api.entity.Client;
import org.springframework.stereotype.Component;

/**
 * Mapper pour convertir entre l'entité {@link Client} et ses DTOs.
 */
@Component
public class ClientMapper {

    /**
     * Convertit un DTO de requête en entité (pour CREATE).
     */
    public Client toEntity(ClientRequestDTO dto) {
        if (dto == null) return null;

        return Client.builder()
                .nom(dto.getNom())
                .prenom(dto.getPrenom())
                .email(dto.getEmail())
                .telephone(dto.getTelephone())
                .adresse(dto.getAdresse())
                .ville(dto.getVille())
                .pays(dto.getPays())
                .siret(dto.getSiret())
                .actif(dto.getActif() == null || dto.getActif())
                .build();
    }

    /**
     * Met à jour une entité existante à partir d'un DTO (pour UPDATE).
     */
    public void updateEntity(Client entity, ClientRequestDTO dto) {
        entity.setNom(dto.getNom());
        entity.setPrenom(dto.getPrenom());
        entity.setEmail(dto.getEmail());
        entity.setTelephone(dto.getTelephone());
        entity.setAdresse(dto.getAdresse());
        entity.setVille(dto.getVille());
        entity.setPays(dto.getPays());
        entity.setSiret(dto.getSiret());
        entity.setActif(dto.getActif() != null ? dto.getActif() : entity.isActif());
    }

    /**
     * Convertit une entité en DTO de réponse.
     */
    public ClientResponseDTO toResponseDTO(Client entity) {
        if (entity == null) return null;

        return ClientResponseDTO.builder()
                .id(entity.getId())
                .nom(entity.getNom())
                .prenom(entity.getPrenom())
                .email(entity.getEmail())
                .telephone(entity.getTelephone())
                .adresse(entity.getAdresse())
                .ville(entity.getVille())
                .pays(entity.getPays())
                .siret(entity.getSiret())
                .actif(entity.isActif())
                .dateCreation(entity.getDateCreation())
                .dateModification(entity.getDateModification())
                .build();
    }
}