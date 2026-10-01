package com.innovatech.api.service;

import com.innovatech.api.dto.PhotoProduitDTO;
import com.innovatech.api.entity.PhotoProduit;
import com.innovatech.api.entity.Produit;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.PhotoProduitMapper;
import com.innovatech.api.repository.PhotoProduitRepository;   // ← ⚠️ CET IMPORT
import com.innovatech.api.repository.ProduitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PhotoProduitService {

    private final PhotoProduitRepository photoRepository;
    private final ProduitRepository produitRepository;
    private final PhotoProduitMapper photoMapper;
    private final FileStorageService fileStorageService;

    private static final int MAX_PHOTOS = 5;
    private static final String SOUS_DOSSIER = "produits";

    // -----------------------------------------
    // READ
    // -----------------------------------------

    public List<PhotoProduitDTO> findByProduit(Long produitId) {
        if (!produitRepository.existsById(produitId)) {
            throw new ResourceNotFoundException("Produit", produitId);
        }
        return photoRepository.findByProduitIdOrderByOrdreAsc(produitId)
                .stream()
                .map(photoMapper::toDTO)
                .toList();
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @Transactional
    public PhotoProduitDTO ajouter(Long produitId, MultipartFile file, String titre, boolean principale) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", produitId));

        // Vérifier le nombre max
        long nbPhotos = photoRepository.countByProduitId(produitId);
        if (nbPhotos >= MAX_PHOTOS) {
            throw new BusinessException(
                    "Maximum " + MAX_PHOTOS + " photos par produit atteint");
        }

        // Stocker le fichier
        String nomFichier = fileStorageService.store(file, SOUS_DOSSIER);

        // Si c'est la première photo, elle devient principale
        boolean estPrincipale = principale || nbPhotos == 0;

        // Si principale → retirer le flag des autres
        if (estPrincipale) {
            photoRepository.findByProduitIdAndPrincipaleTrue(produitId)
                    .ifPresent(p -> {
                        p.setPrincipale(false);
                        photoRepository.save(p);
                    });
        }

        // Créer l'entité
        PhotoProduit photo = PhotoProduit.builder()
                .produit(produit)
                .nomFichier(nomFichier)
                .chemin("/" + SOUS_DOSSIER + "/" + nomFichier)
                .typeMime(file.getContentType())
                .taille(file.getSize())
                .titre(titre)
                .principale(estPrincipale)
                .ordre((int) nbPhotos)
                .build();

        PhotoProduit saved = photoRepository.save(photo);
        log.info("Photo ajoutée au produit {} : {}", produitId, nomFichier);
        return photoMapper.toDTO(saved);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @Transactional
    public PhotoProduitDTO setPrincipale(Long photoId) {
        PhotoProduit photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", photoId));

        Long produitId = photo.getProduit().getId();

        // Retirer le flag de toutes les photos du produit
        photoRepository.findByProduitIdOrderByOrdreAsc(produitId)
                .forEach(p -> {
                    if (p.isPrincipale()) {
                        p.setPrincipale(false);
                        photoRepository.save(p);
                    }
                });

        // Marquer celle-ci comme principale
        photo.setPrincipale(true);
        PhotoProduit updated = photoRepository.save(photo);
        return photoMapper.toDTO(updated);
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------

    @Transactional
    public void supprimer(Long photoId) {
        PhotoProduit photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", photoId));

        Long produitId = photo.getProduit().getId();
        boolean etaitPrincipale = photo.isPrincipale();

        // Supprimer le fichier physique
        fileStorageService.delete(photo.getNomFichier(), SOUS_DOSSIER);

        // Supprimer l'entité
        photoRepository.delete(photo);

        // Si c'était la principale et qu'il reste des photos → la 1ʳᵉ devient principale
        if (etaitPrincipale) {
            List<PhotoProduit> restantes = photoRepository.findByProduitIdOrderByOrdreAsc(produitId);
            if (!restantes.isEmpty()) {
                PhotoProduit premiere = restantes.get(0);
                premiere.setPrincipale(true);
                photoRepository.save(premiere);
            }
        }

        log.info("Photo {} supprimée", photoId);
    }
}