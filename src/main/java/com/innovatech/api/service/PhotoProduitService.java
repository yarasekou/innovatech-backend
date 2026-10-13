package com.innovatech.api.service;

import com.innovatech.api.dto.PhotoProduitDTO;
import com.innovatech.api.entity.Entreprise;
import com.innovatech.api.entity.PhotoProduit;
import com.innovatech.api.entity.Produit;
import com.innovatech.api.entity.Role;
import com.innovatech.api.entity.User;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.PhotoProduitMapper;
import com.innovatech.api.repository.PhotoProduitRepository;
import com.innovatech.api.repository.ProduitRepository;
import com.innovatech.api.repository.UserRepository;
import com.innovatech.api.security.SecurityUtils;
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
    private final UserRepository userRepository;
    private final PhotoProduitMapper photoMapper;
    private final FileStorageService fileStorageService;

    private static final int MAX_PHOTOS = 5;
    private static final String SOUS_DOSSIER = "produits";

    // -----------------------------------------
    // READ
    // -----------------------------------------

    public List<PhotoProduitDTO> findByProduit(Long produitId) {
        Produit produit = findMyProduit(produitId);

        return photoRepository.findByProduitIdOrderByOrdreAsc(produit.getId())
                .stream()
                .map(photoMapper::toDTO)
                .toList();
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @Transactional
    public PhotoProduitDTO ajouter(Long produitId, MultipartFile file, String titre, boolean principale) {
        // ⚠️ Vérifie que le produit appartient à la boutique de l'utilisateur
        Produit produit = findMyProduit(produitId);

        long nbPhotos = photoRepository.countByProduitId(produit.getId());
        if (nbPhotos >= MAX_PHOTOS) {
            throw new BusinessException(
                    "Maximum " + MAX_PHOTOS + " photos par produit atteint");
        }

        String nomFichier = fileStorageService.store(file, SOUS_DOSSIER);

        boolean estPrincipale = principale || nbPhotos == 0;

        if (estPrincipale) {
            photoRepository.findByProduitIdAndPrincipaleTrue(produit.getId())
                    .ifPresent(p -> {
                        p.setPrincipale(false);
                        photoRepository.save(p);
                    });
        }

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

        // ⚠️ Vérifie que le produit appartient à la boutique
        findMyProduit(photo.getProduit().getId());

        Long produitId = photo.getProduit().getId();

        photoRepository.findByProduitIdOrderByOrdreAsc(produitId)
                .forEach(p -> {
                    if (p.isPrincipale()) {
                        p.setPrincipale(false);
                        photoRepository.save(p);
                    }
                });

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

        // ⚠️ Vérifie que le produit appartient à la boutique
        findMyProduit(photo.getProduit().getId());

        Long produitId = photo.getProduit().getId();
        boolean etaitPrincipale = photo.isPrincipale();

        fileStorageService.delete(photo.getNomFichier(), SOUS_DOSSIER);
        photoRepository.delete(photo);

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

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    /**
     * Récupère un produit en vérifiant qu'il appartient à la boutique
     * de l'utilisateur connecté.
     * <ul>
     *   <li>ADMIN  → peut accéder à tous les produits de son entreprise</li>
     *   <li>USER   → seulement ses produits</li>
     *   <li>SELLER → seulement les produits de son patron</li>
     * </ul>
     */
    private Produit findMyProduit(Long produitId) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            // ADMIN → peut voir tous les produits de son entreprise
            return produitRepository
                    .findByIdAndEntrepriseIdWithDetails(produitId, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", produitId));
        }

        // USER / SELLER → seulement sa boutique
        Long shopOwnerId = resolveShopOwnerId(user);
        return produitRepository
                .findByIdAndEntrepriseIdAndUserIdWithDetails(
                        produitId, entreprise.getId(), shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", produitId));
    }

    private Long resolveShopOwnerId(User user) {
        if (user.hasRole(Role.ROLE_SELLER)) {
            if (user.getPatron() == null) {
                throw new BusinessException("Ce SELLER n'a pas de patron assigné");
            }
            return user.getPatron().getId();
        }
        return user.getId();
    }

    private Entreprise getCurrentEntreprise(User user) {
        if (user.getEntreprise() == null) {
            throw new BusinessException(
                    "Aucune entreprise associée. Contactez votre administrateur.");
        }
        return user.getEntreprise();
    }

    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null) {
            throw new BusinessException("Utilisateur non authentifié");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User introuvable avec l'email : " + email));
    }
}