package com.innovatech.api.service;

import com.innovatech.api.dto.ProduitRequestDTO;
import com.innovatech.api.dto.ProduitResponseDTO;
import com.innovatech.api.entity.Categorie;
import com.innovatech.api.entity.Produit;
import com.innovatech.api.entity.Role;
import com.innovatech.api.entity.User;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.ProduitMapper;
import com.innovatech.api.repository.CategorieRepository;
import com.innovatech.api.repository.ProduitRepository;
import com.innovatech.api.repository.UserRepository;
import com.innovatech.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProduitService {

    private final ProduitRepository produitRepository;
    private final CategorieRepository categorieRepository;
    private final UserRepository userRepository;
    private final ProduitMapper produitMapper;

    // -----------------------------------------
    // READ — avec filtrage par user
    // -----------------------------------------

    public List<ProduitResponseDTO> findAll() {
        if (SecurityUtils.isAdmin()) {
            return produitRepository.findAllWithDetails()
                    .stream()
                    .map(produitMapper::toResponseDTO)
                    .toList();
        }

        Long dataOwnerId = getCurrentDataOwnerId();
        return produitRepository.findByUserIdWithDetails(dataOwnerId)
                .stream()
                .map(produitMapper::toResponseDTO)
                .toList();
    }

    public List<ProduitResponseDTO> findAllActifs() {
        if (SecurityUtils.isAdmin()) {
            return produitRepository.findByActifTrueWithDetails()
                    .stream()
                    .map(produitMapper::toResponseDTO)
                    .toList();
        }

        Long userId = getCurrentUserId();
        return produitRepository.findByUserIdAndActifTrueWithDetails(userId)
                .stream()
                .map(produitMapper::toResponseDTO)
                .toList();
    }

    public ProduitResponseDTO findById(Long id) {
        Produit produit;

        if (SecurityUtils.isAdmin()) {
            // Admin peut voir n'importe quel produit
            produit = produitRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        } else {
            // Les autres ne voient que leurs produits
            Long userId = getCurrentUserId();
            produit = produitRepository.findByIdAndUserIdWithDetails(id, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        }

        return produitMapper.toResponseDTO(produit);
    }

    public ProduitResponseDTO findByReference(String reference) {
        Produit produit = produitRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produit avec référence " + reference + " introuvable"));

        // Vérifier les droits (admin OU propriétaire)
        if (!SecurityUtils.isAdmin()) {
            Long userId = getCurrentUserId();
            if (produit.getUser() == null || !produit.getUser().getId().equals(userId)) {
                throw new ResourceNotFoundException("Produit avec référence " + reference + " introuvable");
            }
        }

        return produitMapper.toResponseDTO(produit);
    }

    public List<ProduitResponseDTO> search(String query) {
        if (SecurityUtils.isAdmin()) {
            return produitRepository.findByNomContainingIgnoreCase(query)
                    .stream()
                    .map(produitMapper::toResponseDTO)
                    .toList();
        }

        Long userId = getCurrentUserId();
        return produitRepository.searchByUserId(userId, query)
                .stream()
                .map(produitMapper::toResponseDTO)
                .toList();
    }

    public List<ProduitResponseDTO> findStockFaible(Integer seuil) {
        // ⚠️ Le stock faible concerne les produits du user (ou tous pour admin)
        if (SecurityUtils.isAdmin()) {
            return produitRepository.findByStockLessThan(seuil)
                    .stream()
                    .map(produitMapper::toResponseDTO)
                    .toList();
        }

        Long userId = getCurrentUserId();
        // Filtre supplémentaire sur le user
        return produitRepository.findByStockLessThan(seuil)
                .stream()
                .filter(p -> p.getUser() != null && p.getUser().getId().equals(userId))
                .map(produitMapper::toResponseDTO)
                .toList();
    }

    public List<ProduitResponseDTO> findByCategorie(Long categorieId, boolean includeDescendants) {
        if (!categorieRepository.existsById(categorieId)) {
            throw new ResourceNotFoundException("Categorie", categorieId);
        }

        List<Produit> produits = includeDescendants
                ? produitRepository.findByCategorieAndDescendants(categorieId)
                : produitRepository.findByCategorieId(categorieId);

        // Filtrage supplémentaire si non-admin
        if (!SecurityUtils.isAdmin()) {
            Long userId = getCurrentUserId();
            produits = produits.stream()
                    .filter(p -> p.getUser() != null && p.getUser().getId().equals(userId))
                    .toList();
        }

        return produits.stream()
                .map(produitMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // CREATE — user injecté depuis le JWT
    // -----------------------------------------

    @Transactional
    public ProduitResponseDTO create(ProduitRequestDTO dto, String userEmail) {
        if (produitRepository.existsByReference(dto.getReference())) {
            // ⚠️ Attention : la contrainte est (reference, user_id)
            // Donc on vérifie l'unicité PAR user
            Long userId = getCurrentUserId();
            boolean exists = produitRepository.existsByReferenceAndUserId(dto.getReference(), userId);
            if (exists) {
                throw new BusinessException(
                        "Vous avez déjà un produit avec la référence " + dto.getReference());
            }
        }

        Categorie categorie = resolveCategorie(dto.getCategorieId());
        User user = resolveUser(userEmail);

        Produit produit = produitMapper.toEntity(dto, categorie, user);
        Produit saved = produitRepository.save(produit);
        log.info("Produit {} créé par user {}", saved.getReference(), userEmail);
        return produitMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // UPDATE — avec contrôle d'accès
    // -----------------------------------------

    @Transactional
    public ProduitResponseDTO update(Long id, ProduitRequestDTO dto) {
        Produit existing;

        if (SecurityUtils.isAdmin()) {
            existing = produitRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        } else {
            Long userId = getCurrentUserId();
            existing = produitRepository.findByIdAndUserIdWithDetails(id, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        }

        // Vérifier l'unicité de la référence (par user)
        if (!existing.getReference().equals(dto.getReference())) {
            Long userId = existing.getUser() != null ? existing.getUser().getId() : null;
            boolean exists = produitRepository.existsByReferenceAndUserId(dto.getReference(), userId);
            if (exists) {
                throw new BusinessException(
                        "Vous avez déjà un produit avec la référence " + dto.getReference());
            }
        }

        Categorie categorie = resolveCategorie(dto.getCategorieId());

        produitMapper.updateEntity(existing, dto, categorie);
        Produit updated = produitRepository.save(existing);
        return produitMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // DELETE — avec contrôle d'accès
    // -----------------------------------------

    @Transactional
    public void delete(Long id) {
        Produit produit;

        if (SecurityUtils.isAdmin()) {
            produit = produitRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        } else {
            Long userId = getCurrentUserId();
            produit = produitRepository.findByIdAndUserIdWithDetails(id, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        }

        produitRepository.delete(produit);
        log.info("Produit {} supprimé", produit.getReference());
    }

    @Transactional
    public void deactivate(Long id) {
        Produit produit;

        if (SecurityUtils.isAdmin()) {
            produit = produitRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        } else {
            Long userId = getCurrentUserId();
            produit = produitRepository.findByIdAndUserIdWithDetails(id, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        }

        produit.setActif(false);
        produitRepository.save(produit);
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    private Categorie resolveCategorie(Long categorieId) {
        if (categorieId == null) {
            return null;
        }
        return categorieRepository.findById(categorieId)
                .orElseThrow(() -> new ResourceNotFoundException("Categorie", categorieId));
    }

    private User resolveUser(String userEmail) {
        if (userEmail == null) {
            return null;
        }
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User avec email " + userEmail));
    }

    /**
     * Récupère l'ID du user actuellement connecté.
     */
    private Long getCurrentUserId() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null) {
            throw new BusinessException("Utilisateur non authentifié");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User introuvable avec l'email : " + email))
                .getId();
    }

    /**
     * Récupère l'ID du user dont on doit filtrer les données.
     * <p>
     * - ADMIN  : null (pas de filtre, voit tout)
     * - USER   : son propre ID
     * - SELLER : l'ID de son patron (il voit les données du patron)
     */
    private Long getCurrentDataOwnerId() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null) {
            throw new BusinessException("Utilisateur non authentifié");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User introuvable avec l'email : " + email));

        // Si SELLER → retourner l'ID du patron
        if (user.getRoles().contains(Role.ROLE_SELLER)) {
            if (user.getPatron() == null) {
                throw new BusinessException(
                        "Ce SELLER n'a pas de patron assigné");
            }
            return user.getPatron().getId();
        }

        // Sinon (USER) → son propre ID
        return user.getId();
    }
}