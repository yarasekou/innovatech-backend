package com.innovatech.api.mapper;

import com.innovatech.api.dto.EntrepriseRequestDTO;
import com.innovatech.api.dto.EntrepriseResponseDTO;
import com.innovatech.api.entity.Entreprise;
import com.innovatech.api.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EntrepriseMapper {

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    public Entreprise toEntity(EntrepriseRequestDTO dto, User owner) {
        if (dto == null) return null;

        return Entreprise.builder()
                .nom(dto.getNom())
                .description(dto.getDescription())
                .email(dto.getEmail())
                .telephone(dto.getTelephone())
                .adresse(dto.getAdresse())
                .ville(dto.getVille())
                .pays(dto.getPays() != null ? dto.getPays() : "Sénégal")
                .siret(dto.getSiret())
                .devise(dto.getDevise() != null ? dto.getDevise() : "XOF")
                .tvaPercent(dto.getTvaPercent() != null
                        ? dto.getTvaPercent()
                        : new java.math.BigDecimal("18.00"))
                .actif(true)
                .owner(owner)
                .build();
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    public void updateEntity(Entreprise entity, EntrepriseRequestDTO dto) {
        entity.setNom(dto.getNom());
        entity.setDescription(dto.getDescription());
        entity.setEmail(dto.getEmail());
        entity.setTelephone(dto.getTelephone());
        entity.setAdresse(dto.getAdresse());
        entity.setVille(dto.getVille());
        entity.setPays(dto.getPays() != null ? dto.getPays() : entity.getPays());
        entity.setSiret(dto.getSiret());
        entity.setDevise(dto.getDevise() != null ? dto.getDevise() : entity.getDevise());
        entity.setTvaPercent(dto.getTvaPercent() != null
                ? dto.getTvaPercent()
                : entity.getTvaPercent());
        // ⚠️ Le slug est géré dans le service (car il dépend du nom)
        // ⚠️ L'owner ne change PAS
        // ⚠️ Le logo se gère via un endpoint dédié
    }

    // -----------------------------------------
    // RESPONSE
    // -----------------------------------------

    public EntrepriseResponseDTO toResponseDTO(Entreprise entity) {
        if (entity == null) return null;

        EntrepriseResponseDTO.EntrepriseResponseDTOBuilder builder =
                EntrepriseResponseDTO.builder()
                        .id(entity.getId())
                        .nom(entity.getNom())
                        .slug(entity.getSlug())
                        .description(entity.getDescription())
                        .email(entity.getEmail())
                        .telephone(entity.getTelephone())
                        .adresse(entity.getAdresse())
                        .ville(entity.getVille())
                        .pays(entity.getPays())
                        .siret(entity.getSiret())
                        .devise(entity.getDevise())
                        .tvaPercent(entity.getTvaPercent())
                        .logo(entity.getLogo())
                        .actif(entity.isActif())
                        .dateCreation(entity.getDateCreation())
                        .dateModification(entity.getDateModification());

        // URL du logo
        if (entity.getLogo() != null && !entity.getLogo().isBlank()) {
            builder.logoUrl(contextPath + "/" + uploadDir + "/entreprises/" + entity.getLogo());
        }

        // Owner (ADMIN propriétaire)
        if (entity.getOwner() != null) {
            builder.ownerId(entity.getOwner().getId())
                    .ownerEmail(entity.getOwner().getEmail())
                    .ownerNom(entity.getOwner().getNom())
                    .ownerPrenom(entity.getOwner().getPrenom());
        }

        return builder.build();
    }
}