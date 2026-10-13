package com.innovatech.api.mapper;

import com.innovatech.api.dto.CategorieRequestDTO;
import com.innovatech.api.dto.CategorieResponseDTO;
import com.innovatech.api.dto.CategorieTreeDTO;
import com.innovatech.api.entity.Categorie;
import com.innovatech.api.entity.Entreprise;
import com.innovatech.api.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategorieMapper {

    public Categorie toEntity(CategorieRequestDTO dto,
                              Entreprise entreprise,
                              User user,
                              Categorie parent,
                              String slug,
                              Integer niveau,
                              String cheminComplet) {
        if (dto == null) return null;

        return Categorie.builder()
                .nom(dto.getNom())
                .slug(slug)
                .description(dto.getDescription())
                .entreprise(entreprise)
                .user(user)
                .parent(parent)
                .niveau(niveau)
                .cheminComplet(cheminComplet)
                .actif(dto.getActif() != null ? dto.getActif() : true)
                .build();
    }

    public void updateEntity(Categorie entity,
                             CategorieRequestDTO dto,
                             Categorie parent,
                             String slug,
                             Integer niveau,
                             String cheminComplet) {
        entity.setNom(dto.getNom());
        entity.setSlug(slug);
        entity.setDescription(dto.getDescription());
        entity.setParent(parent);
        entity.setNiveau(niveau);
        entity.setCheminComplet(cheminComplet);
        entity.setActif(dto.getActif() != null ? dto.getActif() : entity.isActif());
        // ⚠️ Entreprise / user ne changent PAS
    }

    public CategorieResponseDTO toResponseDTO(Categorie entity) {
        if (entity == null) return null;

        CategorieResponseDTO.CategorieResponseDTOBuilder builder = CategorieResponseDTO.builder()
                .id(entity.getId())
                .nom(entity.getNom())
                .slug(entity.getSlug())
                .description(entity.getDescription())
                .parentId(entity.getParent() != null ? entity.getParent().getId() : null)
                .parentNom(entity.getParent() != null ? entity.getParent().getNom() : null)
                .niveau(entity.getNiveau())
                .cheminComplet(entity.getCheminComplet())
                .actif(entity.isActif())
                .dateCreation(entity.getDateCreation())
                .dateModification(entity.getDateModification());

        if (entity.getEntreprise() != null) {
            builder.entrepriseId(entity.getEntreprise().getId())
                    .entrepriseNom(entity.getEntreprise().getNom());
        }

        if (entity.getUser() != null) {
            builder.userId(entity.getUser().getId())
                    .userEmail(entity.getUser().getEmail());
        }

        return builder.build();
    }

    public CategorieTreeDTO toTreeDTO(Categorie entity) {
        if (entity == null) return null;

        CategorieTreeDTO dto = CategorieTreeDTO.builder()
                .id(entity.getId())
                .nom(entity.getNom())
                .slug(entity.getSlug())
                .niveau(entity.getNiveau())
                .cheminComplet(entity.getCheminComplet())
                .actif(entity.isActif())
                .build();

        if (entity.getEnfants() != null) {
            List<CategorieTreeDTO> enfantsDTO = entity.getEnfants().stream()
                    .map(this::toTreeDTO)
                    .toList();
            dto.setEnfants(enfantsDTO);
        }

        return dto;
    }
}