package com.innovatech.api.service;

import com.innovatech.api.dto.ProduitRequestDTO;
import com.innovatech.api.dto.ProduitResponseDTO;
import com.innovatech.api.entity.Categorie;
import com.innovatech.api.entity.Entreprise;
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

/**
 * Service de gestion des produits.
 * <p>
 * Filtrage triple selon le rôle :
 * <ul>
 *   <li>ADMIN  → tous les produits de son entreprise</li>
 *   <li>USER   → seulement SA boutique (user_id = lui)</li>
 *   <li>SELLER → la boutique de son patron (user_id = patron)</li>
 * </ul>
 * <p>
 * ⚠️ Un ADMIN ne crée PAS de produit. Ce sont les USERs / SELLERs.
 */
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
    // READ
    // -----------------------------------------

    public List<ProduitResponseDTO> findAll() {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        // ADMIN → tous les produits de l'entreprise
        if (user.hasRole(Role.ROLE_ADMIN)) {
            return produitRepository.findByEntrepriseIdWithDetails(entreprise.getId())
                    .stream()
                    .map(produitMapper::toResponseDTO)
                    .toList();
        }

        // USER / SELLER → produits de la boutique
        Long shopOwnerId = resolveShopOwnerId(user);
        return produitRepository.findByEntrepriseIdAndUserIdWithDetails(
                        entreprise.getId(), shopOwnerId)
                .stream()
                .map(produitMapper::toResponseDTO)
                .toList();
    }

    public List<ProduitResponseDTO> findAllActifs() {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return produitRepository.findByEntrepriseIdAndActifTrueWithDetails(entreprise.getId())
                    .stream()
                    .map(produitMapper::toResponseDTO)
                    .toList();
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return produitRepository.findByEntrepriseIdAndUserIdAndActifTrueWithDetails(
                        entreprise.getId(), shopOwnerId)
                .stream()
                .map(produitMapper::toResponseDTO)
                .toList();
    }

    public ProduitResponseDTO findById(Long id) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Produit produit;

        if (user.hasRole(Role.ROLE_ADMIN)) {
            produit = produitRepository
                    .findByIdAndEntrepriseIdWithDetails(id, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        } else {
            Long shopOwnerId = resolveShopOwnerId(user);
            produit = produitRepository
                    .findByIdAndEntrepriseIdAndUserIdWithDetails(
                            id, entreprise.getId(), shopOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        }

        return produitMapper.toResponseDTO(produit);
    }

    public ProduitResponseDTO findByReference(String reference) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Produit produit;

        if (user.hasRole(Role.ROLE_ADMIN)) {
            produit = produitRepository
                    .findByReferenceAndEntrepriseId(reference, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Produit avec référence " + reference + " introuvable"));
        } else {
            Long shopOwnerId = resolveShopOwnerId(user);
            produit = produitRepository
                    .findByReferenceAndEntrepriseIdAndUserId(
                            reference, entreprise.getId(), shopOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Produit avec référence " + reference + " introuvable"));
        }

        return produitMapper.toResponseDTO(produit);
    }

    public List<ProduitResponseDTO> search(String query) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return produitRepository.searchByEntrepriseId(entreprise.getId(), query)
                    .stream()
                    .map(produitMapper::toResponseDTO)
                    .toList();
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return produitRepository.searchByEntrepriseIdAndUserId(
                        entreprise.getId(), shopOwnerId, query)
                .stream()
                .map(produitMapper::toResponseDTO)
                .toList();
    }

    public List<ProduitResponseDTO> findStockFaible(Integer seuil) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return produitRepository.findByEntrepriseIdAndStockLessThan(
                            entreprise.getId(), seuil)
                    .stream()
                    .map(produitMapper::toResponseDTO)
                    .toList();
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return produitRepository.findByEntrepriseIdAndUserIdAndStockLessThan(
                        entreprise.getId(), shopOwnerId, seuil)
                .stream()
                .map(produitMapper::toResponseDTO)
                .toList();
    }

    public List<ProduitResponseDTO> findByCategorie(Long categorieId, boolean includeDescendants) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        // Vérifier que la catégorie appartient à l'entreprise
        categorieRepository.findByIdAndEntrepriseIdWithParent(categorieId, entreprise.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Categorie", categorieId));

        // Récupérer les produits de la catégorie
        List<Produit> produits = includeDescendants
                ? produitRepository.findByCategorieAndDescendants(categorieId, entreprise.getId())
                : produitRepository.findByEntrepriseIdAndCategorieId(entreprise.getId(), categorieId);

        // Filtrer selon le rôle
        if (!user.hasRole(Role.ROLE_ADMIN)) {
            Long shopOwnerId = resolveShopOwnerId(user);
            produits = produits.stream()
                    .filter(p -> p.getUser() != null
                            && p.getUser().getId().equals(shopOwnerId))
                    .toList();
        }

        return produits.stream()
                .map(produitMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // CREATE — USER / SELLER uniquement
    // -----------------------------------------

    @Transactional
    public ProduitResponseDTO create(ProduitRequestDTO dto, String userEmail) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        // ⚠️ Un ADMIN ne crée pas de produits
        if (user.hasRole(Role.ROLE_ADMIN)) {
            throw new BusinessException(
                    "Un ADMIN ne crée pas de produits. Créez un USER pour cela.");
        }

        // Déterminer le propriétaire de la boutique
        Long shopOwnerId = resolveShopOwnerId(user);
        User shopOwner = userRepository.findById(shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("User (boutique)", shopOwnerId));

        // Vérifier unicité (par entreprise + user boutique)
        if (produitRepository.existsByReferenceAndEntrepriseIdAndUserId(
                dto.getReference(), entreprise.getId(), shopOwnerId)) {
            throw new BusinessException(
                    "Vous avez déjà un produit avec la référence " + dto.getReference());
        }

        Categorie categorie = resolveCategorie(dto.getCategorieId(), entreprise.getId());

        Produit produit = produitMapper.toEntity(dto, entreprise, categorie, shopOwner);
        Produit saved = produitRepository.save(produit);
        log.info("Produit {} créé par {} pour la boutique de {} (entreprise: {})",
                saved.getReference(), userEmail, shopOwner.getEmail(), entreprise.getNom());
        return produitMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @Transactional
    public ProduitResponseDTO update(Long id, ProduitRequestDTO dto) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Produit existing = findMyProduct(user, entreprise, id);

        // Vérifier unicité si la référence change
        Long shopOwnerId = existing.getUser() != null ? existing.getUser().getId() : null;
        if (shopOwnerId != null
                && !existing.getReference().equals(dto.getReference())
                && produitRepository.existsByReferenceAndEntrepriseIdAndUserId(
                dto.getReference(), entreprise.getId(), shopOwnerId)) {
            throw new BusinessException(
                    "Vous avez déjà un produit avec la référence " + dto.getReference());
        }

        Categorie categorie = resolveCategorie(dto.getCategorieId(), entreprise.getId());

        produitMapper.updateEntity(existing, dto, categorie);
        Produit updated = produitRepository.save(existing);
        return produitMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // DELETE / DEACTIVATE
    // -----------------------------------------

    @Transactional
    public void delete(Long id) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Produit produit = findMyProduct(user, entreprise, id);
        produitRepository.delete(produit);
        log.info("Produit {} supprimé", produit.getReference());
    }

    @Transactional
    public void deactivate(Long id) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Produit produit = findMyProduct(user, entreprise, id);
        produit.setActif(false);
        produitRepository.save(produit);
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    /**
     * Retourne l'ID du propriétaire de la boutique.
     * <ul>
     *   <li>SELLER → son patron (USER)</li>
     *   <li>USER   → son propre ID</li>
     * </ul>
     */
    private Long resolveShopOwnerId(User user) {
        if (user.hasRole(Role.ROLE_SELLER)) {
            if (user.getPatron() == null) {
                throw new BusinessException("Ce SELLER n'a pas de patron assigné");
            }
            return user.getPatron().getId();
        }
        return user.getId();
    }

    /**
     * Récupère un produit en respectant le filtrage par rôle.
     */
    private Produit findMyProduct(User user, Entreprise entreprise, Long id) {
        if (user.hasRole(Role.ROLE_ADMIN)) {
            return produitRepository
                    .findByIdAndEntrepriseIdWithDetails(id, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        }
        Long shopOwnerId = resolveShopOwnerId(user);
        return produitRepository
                .findByIdAndEntrepriseIdAndUserIdWithDetails(
                        id, entreprise.getId(), shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
    }

    private Categorie resolveCategorie(Long categorieId, Long entrepriseId) {
        if (categorieId == null) return null;
        return categorieRepository
                .findByIdAndEntrepriseIdWithParent(categorieId, entrepriseId)
                .orElseThrow(() -> new ResourceNotFoundException("Categorie", categorieId));
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