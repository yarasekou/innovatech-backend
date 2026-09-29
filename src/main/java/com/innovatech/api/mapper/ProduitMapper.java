package com.innovatech.api.mapper;

import com.innovatech.api.dto.ProduitRequestDTO;
import com.innovatech.api.dto.ProduitResponseDTO;
import com.innovatech.api.entity.Categorie;
import com.innovatech.api.entity.Produit;
import org.springframework.stereotype.Component;

@Component
public class ProduitMapper {

    public Produit toEntity(ProduitRequestDTO dto, Categorie categorie) {
        if (dto == null) return null;

        return Produit.builder()
                .reference(dto.getReference())
                .nom(dto.getNom())
                .description(dto.getDescription())
                .prix(dto.getPrix())
                .prixAchat(dto.getPrixAchat() != null ? dto.getPrixAchat() : java.math.BigDecimal.ZERO)
                .stock(dto.getStock())
                .categorie(categorie)
                .actif(dto.getActif() == null || dto.getActif())
                .build();
    }

    public void updateEntity(Produit entity, ProduitRequestDTO dto, Categorie categorie) {
        entity.setReference(dto.getReference());
        entity.setNom(dto.getNom());
        entity.setDescription(dto.getDescription());
        entity.setPrix(dto.getPrix());
        entity.setPrixAchat(dto.getPrixAchat() != null ? dto.getPrixAchat() : entity.getPrixAchat());
        entity.setStock(dto.getStock());
        entity.setCategorie(categorie);
        entity.setActif(dto.getActif() != null ? dto.getActif() : entity.isActif());
    }

    public ProduitResponseDTO toResponseDTO(Produit entity) {
        if (entity == null) return null;

        ProduitResponseDTO.ProduitResponseDTOBuilder builder = ProduitResponseDTO.builder()
                .id(entity.getId())
                .reference(entity.getReference())
                .nom(entity.getNom())
                .description(entity.getDescription())
                .prix(entity.getPrix())
                .prixAchat(entity.getPrixAchat())
                .margeUnitaire(entity.getMargeUnitaire())
                .tauxMarge(entity.getTauxMarge())
                .stock(entity.getStock())
                .actif(entity.isActif())
                .dateCreation(entity.getDateCreation())
                .dateModification(entity.getDateModification());

        // Catégorie
        if (entity.getCategorie() != null) {
            Categorie cat = entity.getCategorie();
            builder.categorieId(cat.getId())
                    .categorieNom(cat.getNom())
                    .categorieSlug(cat.getSlug())
                    .categorieChemin(cat.getCheminComplet())
                    .categorieNiveau(cat.getNiveau());
        }

        return builder.build();
    }
}