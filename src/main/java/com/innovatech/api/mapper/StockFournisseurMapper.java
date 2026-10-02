package com.innovatech.api.mapper;

import com.innovatech.api.dto.StockFournisseurDTO;
import com.innovatech.api.entity.StockFournisseur;
import org.springframework.stereotype.Component;

@Component
public class StockFournisseurMapper {

    public StockFournisseurDTO toDTO(StockFournisseur entity) {
        if (entity == null) return null;

        return StockFournisseurDTO.builder()
                .id(entity.getId())
                .produitId(entity.getProduit() != null ? entity.getProduit().getId() : null)
                .produitReference(entity.getProduit() != null ? entity.getProduit().getReference() : null)
                .produitNom(entity.getProduit() != null ? entity.getProduit().getNom() : null)
                .fournisseurId(entity.getFournisseur() != null ? entity.getFournisseur().getId() : null)
                .fournisseurCode(entity.getFournisseur() != null ? entity.getFournisseur().getCode() : null)
                .fournisseurNom(entity.getFournisseur() != null ? entity.getFournisseur().getNom() : null)
                .stock(entity.getStock())
                .prixAchat(entity.getPrixAchat())
                .dateModification(entity.getDateModification())
                .build();
    }
}