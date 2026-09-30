package com.innovatech.api.mapper;

import com.innovatech.api.dto.ClientRequestDTO;
import com.innovatech.api.dto.ClientResponseDTO;
import com.innovatech.api.entity.Client;
import com.innovatech.api.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ClientMapper {

    public Client toEntity(ClientRequestDTO dto, User user) {
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
                .actif(dto.getActif() != null ? dto.getActif() : true)
                .user(user)
                .build();
    }

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
        // ⚠️ On ne touche PAS au user en UPDATE (il reste le créateur)
    }

    public ClientResponseDTO toResponseDTO(Client entity) {
        if (entity == null) return null;

        ClientResponseDTO.ClientResponseDTOBuilder builder = ClientResponseDTO.builder()
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
                .dateModification(entity.getDateModification());

        if (entity.getUser() != null) {
            builder.userId(entity.getUser().getId())
                    .userEmail(entity.getUser().getEmail())
                    .userNom(entity.getUser().getNom())
                    .userPrenom(entity.getUser().getPrenom());
        }

        return builder.build();
    }
}