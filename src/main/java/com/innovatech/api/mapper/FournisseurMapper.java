package com.innovatech.api.mapper;

import com.innovatech.api.dto.FournisseurRequestDTO;
import com.innovatech.api.dto.FournisseurResponseDTO;
import com.innovatech.api.entity.Fournisseur;
import org.springframework.stereotype.Component;
import com.innovatech.api.entity.User;


@Component
public class FournisseurMapper {

    public Fournisseur toEntity(FournisseurRequestDTO dto, User user) {
        if (dto == null) return null;

        return Fournisseur.builder()
                .code(dto.getCode())
                .nom(dto.getNom())
                .email(dto.getEmail())
                .telephone(dto.getTelephone())
                .adresse(dto.getAdresse())
                .ville(dto.getVille())
                .pays(dto.getPays())
                .siret(dto.getSiret())
                .contactNom(dto.getContactNom())
                .actif(dto.getActif() != null ? dto.getActif() : true)
                .user(user)
                .build();
    }

    public void updateEntity(Fournisseur entity, FournisseurRequestDTO dto) {
        entity.setCode(dto.getCode());
        entity.setNom(dto.getNom());
        entity.setEmail(dto.getEmail());
        entity.setTelephone(dto.getTelephone());
        entity.setAdresse(dto.getAdresse());
        entity.setVille(dto.getVille());
        entity.setPays(dto.getPays());
        entity.setSiret(dto.getSiret());
        entity.setContactNom(dto.getContactNom());
        entity.setActif(dto.getActif() != null ? dto.getActif() : entity.isActif());
        // user inchangé
    }

    public FournisseurResponseDTO toResponseDTO(Fournisseur entity) {
        if (entity == null) return null;

        FournisseurResponseDTO.FournisseurResponseDTOBuilder builder = FournisseurResponseDTO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .nom(entity.getNom())
                .email(entity.getEmail())
                .telephone(entity.getTelephone())
                .adresse(entity.getAdresse())
                .ville(entity.getVille())
                .pays(entity.getPays())
                .siret(entity.getSiret())
                .contactNom(entity.getContactNom())
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