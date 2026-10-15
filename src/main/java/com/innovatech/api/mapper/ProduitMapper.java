package com.innovatech.api.mapper;

import com.innovatech.api.dto.PhotoProduitDTO;
import com.innovatech.api.dto.ProduitRequestDTO;
import com.innovatech.api.dto.ProduitResponseDTO;
import com.innovatech.api.entity.Categorie;
import com.innovatech.api.entity.Entreprise;
import com.innovatech.api.entity.PhotoProduit;
import com.innovatech.api.entity.Produit;
import com.innovatech.api.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProduitMapper {

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    public Produit toEntity(ProduitRequestDTO dto,
                            Entreprise entreprise,
                            Categorie categorie,
                            User user) {
        if (dto == null) return null;

        return Produit.builder()
                .reference(dto.getReference())
                .nom(dto.getNom())
                .description(dto.getDescription())
                .prix(dto.getPrix())
                .prixAchat(dto.getPrixAchat() != null
                        ? dto.getPrixAchat()
                        : java.math.BigDecimal.ZERO)
                .stock(dto.getStock())
                .entreprise(entreprise)
                .categorie(categorie)
                .user(user)
                .actif(dto.getActif() != null ? dto.getActif() : true)
                .build();
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    public void updateEntity(Produit entity, ProduitRequestDTO dto, Categorie categorie) {
        entity.setReference(dto.getReference());
        entity.setNom(dto.getNom());
        entity.setDescription(dto.getDescription());
        entity.setPrix(dto.getPrix());
        entity.setPrixAchat(dto.getPrixAchat() != null ? dto.getPrixAchat() : entity.getPrixAchat());
        entity.setStock(dto.getStock());
        entity.setCategorie(categorie);
        entity.setActif(dto.getActif() != null ? dto.getActif() : entity.isActif());
        // ⚠️ Entreprise ne change PAS
        // ⚠️ User ne change PAS
    }

    // -----------------------------------------
    // RESPONSE
    // -----------------------------------------

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

        // Entreprise
        if (entity.getEntreprise() != null) {
            builder.entrepriseId(entity.getEntreprise().getId())
                    .entrepriseNom(entity.getEntreprise().getNom());
        }

        // Catégorie
        if (entity.getCategorie() != null) {
            Categorie cat = entity.getCategorie();
            builder.categorieId(cat.getId())
                    .categorieNom(cat.getNom())
                    .categorieSlug(cat.getSlug())
                    .categorieChemin(cat.getCheminComplet())
                    .categorieNiveau(cat.getNiveau());
        }

        // User créateur
        if (entity.getUser() != null) {
            User user = entity.getUser();
            builder.userId(user.getId())
                    .userEmail(user.getEmail())
                    .userNom(user.getNom())
                    .userPrenom(user.getPrenom());
        }

        // Photos
        if (entity.getPhotos() != null && !entity.getPhotos().isEmpty()) {
            List<PhotoProduitDTO> photosDTO = entity.getPhotos().stream()
                    .map(this::toPhotoDTO)
                    .toList();

            builder.photos(photosDTO)
                    .nbPhotos(photosDTO.size())
                    .photoPrincipale(photosDTO.stream()
                            .filter(PhotoProduitDTO::isPrincipale)
                            .findFirst()
                            .map(PhotoProduitDTO::getUrl)
                            .orElse(photosDTO.get(0).getUrl()));
        } else {
            builder.nbPhotos(0);
        }

        return builder.build();
    }

    private PhotoProduitDTO toPhotoDTO(PhotoProduit photo) {
        return PhotoProduitDTO.builder()
                .id(photo.getId())
                .produitId(photo.getProduit() != null ? photo.getProduit().getId() : null)
                .url(contextPath + "/" + uploadDir + "/produits/" + photo.getNomFichier())   // ✅ FIX
                .nomFichier(photo.getNomFichier())
                .titre(photo.getTitre())
                .principale(photo.isPrincipale())
                .ordre(photo.getOrdre())
                .taille(photo.getTaille())
                .typeMime(photo.getTypeMime())
                .dateCreation(photo.getDateCreation())
                .build();
    }
}