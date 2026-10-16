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

        // Résoudre le parent
        Categorie parent = null;
        if (dto.getParentId() != null) {
            parent = categorieRepository
                    .findByIdAndEntrepriseIdAndUserIdWithParent(
                            dto.getParentId(), entreprise.getId(), shopOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Categorie (parent)", dto.getParentId()));
        }

        // ⚠️ Générer le slug unique par parent
        String slug = genererSlugUnique(
                dto.getNom(),
                dto.getSlug(),
                parent,
                entreprise.getId(),
                shopOwnerId
        );

        int niveau = (parent == null) ? 1 : parent.getNiveau() + 1;
        String cheminComplet = (parent == null)
                ? dto.getNom()
                : parent.getCheminComplet() + " > " + dto.getNom();

        Categorie categorie = categorieMapper.toEntity(
                dto, entreprise, shopOwner, parent, slug, niveau, cheminComplet);

        Categorie saved = categorieRepository.save(categorie);
        log.info("Catégorie '{}' créée par {} pour la boutique de {} (entreprise: {}, slug: {})",
                saved.getNom(), userEmail, shopOwner.getEmail(), entreprise.getNom(), saved.getSlug());
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

            // Protection anti-cycle
            if (parent.getCheminComplet() != null
                    && existing.getCheminComplet() != null
                    && parent.getCheminComplet().startsWith(existing.getCheminComplet() + " > ")) {
                throw new BusinessException(
                        "Impossible : le parent choisi est un descendant de cette catégorie (cycle)");
            }
        }

        // ⚠️ Générer le slug unique par parent (SAUF si le slug n'a pas changé)
        String slug;
        boolean slugChange = dto.getSlug() != null
                && !dto.getSlug().isBlank()
                && !dto.getSlug().equals(existing.getSlug());

        boolean nomChange = !dto.getNom().equals(existing.getNom());
        boolean parentChange = (existing.getParent() == null && parent != null)
                || (existing.getParent() != null && parent == null)
                || (existing.getParent() != null && parent != null
                && !existing.getParent().getId().equals(parent.getId()));

        if (nomChange || slugChange || parentChange) {
            // On doit regénérer le slug
            slug = genererSlugUnique(
                    dto.getNom(),
                    dto.getSlug(),
                    parent,
                    entreprise.getId(),
                    shopOwnerId,
                    existing.getId()   // ← exclure la catégorie courante de l'unicité
            );
        } else {
            // Rien n'a changé → garder le slug existant
            slug = existing.getSlug();
        }

        int niveau = (parent == null) ? 1 : parent.getNiveau() + 1;
        String ancienChemin = existing.getCheminComplet();
        String nouveauChemin = (parent == null)
                ? dto.getNom()
                : parent.getCheminComplet() + " > " + dto.getNom();

        categorieMapper.updateEntity(existing, dto, parent, slug, niveau, nouveauChemin);
        Categorie updated = categorieRepository.save(existing);

        // Recalculer les chemins des descendants
        if (ancienChemin != null && !ancienChemin.equals(nouveauChemin)) {
            recalculerCheminsDescendants(updated, ancienChemin, nouveauChemin);
        }

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

    /**
     * Génère un slug unique **par parent**.
     * <p>
     * - Parent null  → slug simple (ex: "ordinateurs")
     * - Parent XXX   → slug préfixé (ex: "ordinateurs-portables")
     * <p>
     * La vérification d'unicité se fait au niveau du parent :
     * deux catégories "Dell" sous "Portables" et sous "Fixe" auront
     * respectivement "portables-dell" et "fixe-dell".
     */
    private String genererSlugUnique(String nom,
                                     String slugFourni,
                                     Categorie parent,
                                     Long entrepriseId,
                                     Long userId) {
        return genererSlugUnique(nom, slugFourni, parent, entrepriseId, userId, null);
    }

    private String genererSlugUnique(String nom,
                                     String slugFourni,
                                     Categorie parent,
                                     Long entrepriseId,
                                     Long userId,
                                     Long excludeId) {

        // Base : slug fourni ou généré depuis le nom
        String baseSlug = (slugFourni != null && !slugFourni.isBlank())
                ? slugFourni
                : genererSlug(nom);

        // Préfixe : slug du parent si présent
        String slug;
        if (parent == null) {
            slug = baseSlug;
        } else {
            // ⚠️ Utilise le SLUG du parent (pas son nom) pour construire le chemin
            // Ex: parent.slug = "ordinateurs-portables" → enfant = "ordinateurs-portables-dell"
            slug = parent.getSlug() + "-" + baseSlug;
        }

        // Vérification d'unicité au niveau du parent
        boolean exists;
        if (parent == null) {
            exists = categorieRepository
                    .existsBySlugAndEntrepriseIdAndUserIdAndParentIsNull(
                            slug, entrepriseId, userId);
        } else {
            exists = categorieRepository
                    .existsBySlugAndEntrepriseIdAndUserIdAndParentId(
                            slug, entrepriseId, userId, parent.getId());
        }

        // Si on modifie et que le slug trouvé est celui de la catégorie courante → OK
        if (exists && excludeId != null) {
            Categorie bySlug = categorieRepository
                    .findBySlugAndEntrepriseIdAndUserId(slug, entrepriseId, userId)
                    .orElse(null);
            if (bySlug != null && bySlug.getId().equals(excludeId)) {
                return slug;
            }
        }

        if (exists) {
            throw new BusinessException(
                    "Une catégorie avec le slug '" + slug + "' existe déjà à cet emplacement.");
        }

        return slug;
    }

    /**
     * Recalcule récursivement le chemin_complet de tous les descendants
     * d'une catégorie après un renommage ou un changement de parent.
     */
    private void recalculerCheminsDescendants(Categorie parent,
                                              String ancienChemin,
                                              String nouveauChemin) {
        List<Categorie> enfants = categorieRepository
                .findByEntrepriseIdAndUserIdAndParentId(
                        parent.getEntreprise().getId(),
                        parent.getUser().getId(),
                        parent.getId());

        for (Categorie enfant : enfants) {
            String cheminEnfant = ancienChemin + " > " + enfant.getNom();
            String nouveauCheminEnfant = nouveauChemin + " > " + enfant.getNom();

            enfant.setCheminComplet(nouveauCheminEnfant);
            enfant.setNiveau(parent.getNiveau() + 1);
            categorieRepository.save(enfant);

            recalculerCheminsDescendants(enfant, cheminEnfant, nouveauCheminEnfant);
        }
    }

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