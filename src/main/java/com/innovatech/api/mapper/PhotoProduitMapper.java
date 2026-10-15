package com.innovatech.api.mapper;

import com.innovatech.api.dto.PhotoProduitDTO;
import com.innovatech.api.entity.PhotoProduit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PhotoProduitMapper {

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public PhotoProduitDTO toDTO(PhotoProduit entity) {
        if (entity == null) return null;

        return PhotoProduitDTO.builder()
                .id(entity.getId())
                .produitId(entity.getProduit() != null ? entity.getProduit().getId() : null)
                .url(contextPath + "/" + uploadDir + "/produits/" + entity.getNomFichier())
                .nomFichier(entity.getNomFichier())
                .titre(entity.getTitre())
                .principale(entity.isPrincipale())
                .ordre(entity.getOrdre())
                .taille(entity.getTaille())
                .typeMime(entity.getTypeMime())
                .dateCreation(entity.getDateCreation())
                .build();
    }
}