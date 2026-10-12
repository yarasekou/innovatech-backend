package com.innovatech.api.service;

import com.innovatech.api.dto.EntrepriseRequestDTO;
import com.innovatech.api.dto.EntrepriseResponseDTO;
import com.innovatech.api.entity.Entreprise;
import com.innovatech.api.entity.Role;
import com.innovatech.api.entity.User;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.EntrepriseMapper;
import com.innovatech.api.repository.EntrepriseRepository;
import com.innovatech.api.repository.UserRepository;
import com.innovatech.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EntrepriseService {

    private final EntrepriseRepository entrepriseRepository;
    private final UserRepository userRepository;
    private final EntrepriseMapper entrepriseMapper;
    private final FileStorageService fileStorageService;

    private static final String SOUS_DOSSIER = "entreprises";

    // -----------------------------------------
    // READ
    // -----------------------------------------

    /**
     * Récupère l'entreprise du USER connecté.
     * <p>
     * - USER   : son entreprise
     * - SELLER : l'entreprise de son patron
     * - ADMIN  : erreur (utiliser findById)
     */
    public EntrepriseResponseDTO findMyEntreprise() {
        User user = getCurrentUser();

        if (user.getRoles().contains(Role.ROLE_ADMIN)) {
            throw new BusinessException(
                    "Un ADMIN n'a pas d'entreprise. Utilisez /entreprises/{id}.");
        }

        Long ownerId = resolveOwnerId(user);

        Entreprise entreprise = entrepriseRepository.findByUserIdWithUser(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucune entreprise pour cet utilisateur. Créez-en une d'abord."));

        return entrepriseMapper.toResponseDTO(entreprise);
    }

    public EntrepriseResponseDTO findById(Long id) {
        Entreprise entreprise;

        if (SecurityUtils.isAdmin()) {
            entreprise = entrepriseRepository.findByIdWithUser(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Entreprise", id));
        } else {
            User user = getCurrentUser();
            Long ownerId = resolveOwnerId(user);

            entreprise = entrepriseRepository.findByIdWithUser(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Entreprise", id));

            // Vérifier que l'entreprise appartient bien au owner
            if (entreprise.getUser() == null
                    || !entreprise.getUser().getId().equals(ownerId)) {
                throw new ResourceNotFoundException("Entreprise", id);
            }
        }

        return entrepriseMapper.toResponseDTO(entreprise);
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @Transactional
    public EntrepriseResponseDTO create(EntrepriseRequestDTO dto, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User introuvable avec l'email : " + userEmail));

        // Un SELLER ne peut pas créer d'entreprise
        if (user.getRoles().contains(Role.ROLE_SELLER)) {
            throw new BusinessException("Un SELLER ne peut pas créer d'entreprise");
        }

        // Un USER ne peut avoir qu'une seule entreprise
        if (entrepriseRepository.existsByUserId(user.getId())) {
            throw new BusinessException(
                    "Vous avez déjà une entreprise. Utilisez PUT pour la modifier.");
        }

        // Générer un slug unique
        String slug = genererSlug(dto.getNom());
        slug = rendreUnique(slug);

        Entreprise entreprise = entrepriseMapper.toEntity(dto, user);
        entreprise.setSlug(slug);

        Entreprise saved = entrepriseRepository.save(entreprise);
        log.info("Entreprise '{}' créée par {}", saved.getNom(), userEmail);
        return entrepriseMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @Transactional
    public EntrepriseResponseDTO update(Long id, EntrepriseRequestDTO dto) {
        Entreprise existing;

        if (SecurityUtils.isAdmin()) {
            existing = entrepriseRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Entreprise", id));
        } else {
            existing = getMyEntrepriseEntity();
            if (!existing.getId().equals(id)) {
                throw new ResourceNotFoundException("Entreprise", id);
            }
        }

        // Si le nom change, régénérer le slug
        if (!existing.getNom().equals(dto.getNom())) {
            String slug = genererSlug(dto.getNom());
            slug = rendreUnique(slug);
            existing.setSlug(slug);
        }

        entrepriseMapper.updateEntity(existing, dto);
        Entreprise updated = entrepriseRepository.save(existing);
        return entrepriseMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // LOGO
    // -----------------------------------------

    @Transactional
    public EntrepriseResponseDTO uploadLogo(MultipartFile file) {
        Entreprise entreprise = getMyEntrepriseEntity();

        // Supprimer l'ancien logo si existant
        if (entreprise.getLogo() != null && !entreprise.getLogo().isBlank()) {
            fileStorageService.delete(entreprise.getLogo(), SOUS_DOSSIER);
        }

        // Stocker le nouveau
        String nomFichier = fileStorageService.store(file, SOUS_DOSSIER);
        entreprise.setLogo(nomFichier);

        Entreprise updated = entrepriseRepository.save(entreprise);
        log.info("Logo de l'entreprise {} mis à jour : {}", entreprise.getNom(), nomFichier);
        return entrepriseMapper.toResponseDTO(updated);
    }

    @Transactional
    public EntrepriseResponseDTO deleteLogo() {
        Entreprise entreprise = getMyEntrepriseEntity();

        if (entreprise.getLogo() == null) {
            throw new BusinessException("Cette entreprise n'a pas de logo");
        }

        fileStorageService.delete(entreprise.getLogo(), SOUS_DOSSIER);
        entreprise.setLogo(null);

        Entreprise updated = entrepriseRepository.save(entreprise);
        return entrepriseMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------

    @Transactional
    public void delete(Long id) {
        Entreprise entreprise = entrepriseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entreprise", id));

        // Supprimer le logo physique
        if (entreprise.getLogo() != null) {
            fileStorageService.delete(entreprise.getLogo(), SOUS_DOSSIER);
        }

        entrepriseRepository.delete(entreprise);
        log.info("Entreprise {} supprimée", entreprise.getNom());
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null) {
            throw new BusinessException("Utilisateur non authentifié");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User introuvable avec l'email : " + email));
    }

    /**
     * Retourne l'ID du propriétaire de l'entreprise.
     * - USER   : son propre ID
     * - SELLER : l'ID de son patron
     */
    private Long resolveOwnerId(User user) {
        if (user.getRoles().contains(Role.ROLE_SELLER)) {
            if (user.getPatron() == null) {
                throw new BusinessException("Ce SELLER n'a pas de patron assigné");
            }
            return user.getPatron().getId();
        }
        return user.getId();
    }

    private Entreprise getMyEntrepriseEntity() {
        User user = getCurrentUser();
        Long ownerId = resolveOwnerId(user);

        return entrepriseRepository.findByUserId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucune entreprise pour cet utilisateur. Créez-en une d'abord."));
    }

    /**
     * Génère un slug depuis un nom : "Innova Tech SARL" → "innova-tech-sarl"
     */
    private String genererSlug(String nom) {
        if (nom == null) return "entreprise";

        String normalized = Normalizer.normalize(nom, Normalizer.Form.NFD);
        String sansAccents = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        return sansAccents
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    /**
     * Ajoute un suffixe numérique si le slug existe déjà.
     */
    private String rendreUnique(String slug) {
        if (!entrepriseRepository.existsBySlug(slug)) {
            return slug;
        }
        int i = 1;
        String base = slug;
        while (entrepriseRepository.existsBySlug(slug)) {
            slug = base + "-" + i;
            i++;
        }
        return slug;
    }
}