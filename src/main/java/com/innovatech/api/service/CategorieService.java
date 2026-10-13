package com.innovatech.api.service;

import com.innovatech.api.dto.CategorieRequestDTO;
import com.innovatech.api.dto.CategorieResponseDTO;
import com.innovatech.api.dto.CategorieTreeDTO;
import com.innovatech.api.entity.Categorie;
import com.innovatech.api.entity.Entreprise;
import com.innovatech.api.entity.Role;
import com.innovatech.api.entity.User;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.CategorieMapper;
import com.innovatech.api.repository.CategorieRepository;
import com.innovatech.api.repository.UserRepository;
import com.innovatech.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategorieService {

    private final CategorieRepository categorieRepository;
    private final UserRepository userRepository;
    private final CategorieMapper categorieMapper;

    // -----------------------------------------
    // READ
    // -----------------------------------------

    public List<CategorieResponseDTO> findAll() {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return categorieRepository.findByEntrepriseId(entreprise.getId())
                    .stream()
                    .map(categorieMapper::toResponseDTO)
                    .toList();
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return categorieRepository.findByEntrepriseIdAndUserId(
                        entreprise.getId(), shopOwnerId)
                .stream()
                .map(categorieMapper::toResponseDTO)
                .toList();
    }

    public List<CategorieResponseDTO> findRacines() {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return categorieRepository.findByEntrepriseIdAndParentIsNull(entreprise.getId())
                    .stream()
                    .map(categorieMapper::toResponseDTO)
                    .toList();
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return categorieRepository.findByEntrepriseIdAndUserIdAndParentIsNull(
                        entreprise.getId(), shopOwnerId)
                .stream()
                .map(categorieMapper::toResponseDTO)
                .toList();
    }

    public List<CategorieResponseDTO> findByParent(Long parentId) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Categorie parent = findMyCategorie(user, entreprise, parentId);

        Long shopOwnerId = resolveShopOwnerId(user);
        return categorieRepository.findByEntrepriseIdAndUserIdAndParentId(
                        entreprise.getId(), shopOwnerId, parent.getId())
                .stream()
                .map(categorieMapper::toResponseDTO)
                .toList();
    }

    public CategorieResponseDTO findById(Long id) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Categorie categorie = findMyCategorie(user, entreprise, id);
        return categorieMapper.toResponseDTO(categorie);
    }

    public CategorieResponseDTO findBySlug(String slug) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Long shopOwnerId = resolveShopOwnerId(user);
        Categorie categorie = categorieRepository
                .findBySlugAndEntrepriseIdAndUserId(slug, entreprise.getId(), shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categorie avec slug " + slug + " introuvable"));
        return categorieMapper.toResponseDTO(categorie);
    }

    public List<CategorieTreeDTO> getTree() {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        List<Categorie> racines;

        if (user.hasRole(Role.ROLE_ADMIN)) {
            racines = categorieRepository.findByEntrepriseIdAndParentIsNull(entreprise.getId());
        } else {
            Long shopOwnerId = resolveShopOwnerId(user);
            racines = categorieRepository.findByEntrepriseIdAndUserIdAndParentIsNull(
                    entreprise.getId(), shopOwnerId);
        }

        return racines.stream()
                .map(categorieMapper::toTreeDTO)
                .toList();
    }

    public CategorieTreeDTO getTreeFrom(Long id) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Categorie categorie = categorieRepository.findByIdWithEnfants(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categorie", id));

        // Vérifier appartenance
        if (categorie.getEntreprise() == null
                || !categorie.getEntreprise().getId().equals(entreprise.getId())) {
            throw new ResourceNotFoundException("Categorie", id);
        }

        if (!user.hasRole(Role.ROLE_ADMIN)) {
            Long shopOwnerId = resolveShopOwnerId(user);
            if (categorie.getUser() == null
                    || !categorie.getUser().getId().equals(shopOwnerId)) {
                throw new ResourceNotFoundException("Categorie", id);
            }
        }

        return categorieMapper.toTreeDTO(categorie);
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @Transactional
    public CategorieResponseDTO create(CategorieRequestDTO dto, String userEmail) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            throw new BusinessException(
                    "Un ADMIN ne crée pas de catégories. Créez un USER pour cela.");
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        User shopOwner = userRepository.findById(shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("User (boutique)", shopOwnerId));

        // Résoudre le parent (même entreprise + même boutique)
        Categorie parent = null;
        if (dto.getParentId() != null) {
            parent = categorieRepository
                    .findByIdAndEntrepriseIdAndUserIdWithParent(
                            dto.getParentId(), entreprise.getId(), shopOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Categorie (parent)", dto.getParentId()));
        }

        // Slug
        String slug = dto.getSlug() != null && !dto.getSlug().isBlank()
                ? dto.getSlug()
                : genererSlug(dto.getNom());

        if (categorieRepository.existsBySlugAndEntrepriseIdAndUserId(
                slug, entreprise.getId(), shopOwnerId)) {
            throw new BusinessException(
                    "Vous avez déjà une catégorie avec le slug '" + slug + "'");
        }

        int niveau = (parent == null) ? 1 : parent.getNiveau() + 1;
        String cheminComplet = (parent == null)
                ? dto.getNom()
                : parent.getCheminComplet() + " > " + dto.getNom();

        Categorie categorie = categorieMapper.toEntity(
                dto, entreprise, shopOwner, parent, slug, niveau, cheminComplet);

        Categorie saved = categorieRepository.save(categorie);
        log.info("Catégorie '{}' créée par {} pour la boutique de {} (entreprise: {})",
                saved.getNom(), userEmail, shopOwner.getEmail(), entreprise.getNom());
        return categorieMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @Transactional
    public CategorieResponseDTO update(Long id, CategorieRequestDTO dto) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Categorie existing = findMyCategorie(user, entreprise, id);
        Long shopOwnerId = existing.getUser().getId();

        Categorie parent = null;
        if (dto.getParentId() != null) {
            if (dto.getParentId().equals(id)) {
                throw new BusinessException("Une catégorie ne peut pas être son propre parent");
            }
            parent = categorieRepository
                    .findByIdAndEntrepriseIdAndUserIdWithParent(
                            dto.getParentId(), entreprise.getId(), shopOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Categorie (parent)", dto.getParentId()));
        }

        String slug = dto.getSlug() != null && !dto.getSlug().isBlank()
                ? dto.getSlug()
                : genererSlug(dto.getNom());

        if (!existing.getSlug().equals(slug)
                && categorieRepository.existsBySlugAndEntrepriseIdAndUserId(
                slug, entreprise.getId(), shopOwnerId)) {
            throw new BusinessException(
                    "Vous avez déjà une catégorie avec le slug '" + slug + "'");
        }

        int niveau = (parent == null) ? 1 : parent.getNiveau() + 1;
        String cheminComplet = (parent == null)
                ? dto.getNom()
                : parent.getCheminComplet() + " > " + dto.getNom();

        categorieMapper.updateEntity(existing, dto, parent, slug, niveau, cheminComplet);
        Categorie updated = categorieRepository.save(existing);
        return categorieMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------

    @Transactional
    public void delete(Long id) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Categorie categorie = findMyCategorie(user, entreprise, id);
        Long shopOwnerId = categorie.getUser().getId();

        long nbEnfants = categorieRepository.countByEntrepriseIdAndUserIdAndParentId(
                entreprise.getId(), shopOwnerId, id);
        if (nbEnfants > 0) {
            throw new BusinessException(
                    "Impossible de supprimer cette catégorie : elle a " + nbEnfants
                            + " sous-catégorie(s). Supprimez-les d'abord.");
        }

        categorieRepository.delete(categorie);
    }

    // -----------------------------------------
    // FEUILLES
    // -----------------------------------------

    public List<CategorieResponseDTO> findFeuilles() {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return categorieRepository.findFeuillesByEntrepriseId(entreprise.getId())
                    .stream()
                    .map(categorieMapper::toResponseDTO)
                    .toList();
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return categorieRepository.findFeuillesByEntrepriseIdAndUserId(
                        entreprise.getId(), shopOwnerId)
                .stream()
                .map(categorieMapper::toResponseDTO)
                .toList();
    }

    public List<CategorieResponseDTO> findFeuillesDescendants(Long categorieId) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Categorie categorie = findMyCategorie(user, entreprise, categorieId);
        Long shopOwnerId = categorie.getUser().getId();

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return categorieRepository.findFeuillesDescendantsByEntrepriseId(
                            categorieId, entreprise.getId(), categorie.getCheminComplet())
                    .stream()
                    .map(categorieMapper::toResponseDTO)
                    .toList();
        }

        return categorieRepository.findFeuillesDescendantsByEntrepriseIdAndUserId(
                        categorieId, entreprise.getId(), shopOwnerId, categorie.getCheminComplet())
                .stream()
                .map(categorieMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    private String genererSlug(String nom) {
        if (nom == null) return "categorie";

        String normalized = Normalizer.normalize(nom, Normalizer.Form.NFD);
        String sansAccents = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        return sansAccents
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
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

    private Categorie findMyCategorie(User user, Entreprise entreprise, Long id) {
        if (user.hasRole(Role.ROLE_ADMIN)) {
            return categorieRepository
                    .findByIdAndEntrepriseIdWithParent(id, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categorie", id));
        }
        Long shopOwnerId = resolveShopOwnerId(user);
        return categorieRepository
                .findByIdAndEntrepriseIdAndUserIdWithParent(id, entreprise.getId(), shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Categorie", id));
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