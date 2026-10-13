package com.innovatech.api.service;

import com.innovatech.api.dto.FournisseurRequestDTO;
import com.innovatech.api.dto.FournisseurResponseDTO;
import com.innovatech.api.entity.Entreprise;
import com.innovatech.api.entity.Fournisseur;
import com.innovatech.api.entity.Role;
import com.innovatech.api.entity.User;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.FournisseurMapper;
import com.innovatech.api.repository.CommandeAchatRepository;
import com.innovatech.api.repository.FournisseurRepository;
import com.innovatech.api.repository.UserRepository;
import com.innovatech.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service de gestion des fournisseurs.
 * <p>
 * Filtrage triple :
 * <ul>
 *   <li>ADMIN  → tous les fournisseurs de l'entreprise</li>
 *   <li>USER   → seulement SA boutique</li>
 *   <li>SELLER → la boutique de son patron</li>
 * </ul>
 * <p>
 * ⚠️ Un ADMIN ne crée PAS de fournisseur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FournisseurService {

    private final FournisseurRepository fournisseurRepository;
    private final CommandeAchatRepository commandeAchatRepository;
    private final UserRepository userRepository;
    private final FournisseurMapper fournisseurMapper;

    // -----------------------------------------
    // READ
    // -----------------------------------------

    public List<FournisseurResponseDTO> findAll() {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return fournisseurRepository.findByEntrepriseIdWithDetails(entreprise.getId())
                    .stream()
                    .map(fournisseurMapper::toResponseDTO)
                    .toList();
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return fournisseurRepository.findByEntrepriseIdAndUserIdWithDetails(
                        entreprise.getId(), shopOwnerId)
                .stream()
                .map(fournisseurMapper::toResponseDTO)
                .toList();
    }

    public List<FournisseurResponseDTO> findAllActifs() {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return fournisseurRepository.findByEntrepriseIdAndActifTrueWithDetails(entreprise.getId())
                    .stream()
                    .map(fournisseurMapper::toResponseDTO)
                    .toList();
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return fournisseurRepository.findByEntrepriseIdAndUserIdAndActifTrueWithDetails(
                        entreprise.getId(), shopOwnerId)
                .stream()
                .map(fournisseurMapper::toResponseDTO)
                .toList();
    }

    public FournisseurResponseDTO findById(Long id) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Fournisseur fournisseur;

        if (user.hasRole(Role.ROLE_ADMIN)) {
            fournisseur = fournisseurRepository
                    .findByIdAndEntrepriseIdWithDetails(id, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id));
        } else {
            Long shopOwnerId = resolveShopOwnerId(user);
            fournisseur = fournisseurRepository
                    .findByIdAndEntrepriseIdAndUserIdWithDetails(
                            id, entreprise.getId(), shopOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id));
        }

        return fournisseurMapper.toResponseDTO(fournisseur);
    }

    public FournisseurResponseDTO findByCode(String code) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Fournisseur fournisseur;

        if (user.hasRole(Role.ROLE_ADMIN)) {
            fournisseur = fournisseurRepository
                    .findByCodeAndEntrepriseIdAndUserIdWithDetails(code, entreprise.getId(), null)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Fournisseur avec code " + code + " introuvable"));
        } else {
            Long shopOwnerId = resolveShopOwnerId(user);
            fournisseur = fournisseurRepository
                    .findByCodeAndEntrepriseIdAndUserIdWithDetails(
                            code, entreprise.getId(), shopOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Fournisseur avec code " + code + " introuvable"));
        }

        return fournisseurMapper.toResponseDTO(fournisseur);
    }

    public List<FournisseurResponseDTO> search(String query) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return fournisseurRepository.searchByEntrepriseId(entreprise.getId(), query)
                    .stream()
                    .map(fournisseurMapper::toResponseDTO)
                    .toList();
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return fournisseurRepository.searchByEntrepriseIdAndUserId(
                        entreprise.getId(), shopOwnerId, query)
                .stream()
                .map(fournisseurMapper::toResponseDTO)
                .toList();
    }

    public List<FournisseurResponseDTO> findByPays(String pays) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        List<Fournisseur> fournisseurs =
                fournisseurRepository.findByEntrepriseIdAndPays(entreprise.getId(), pays);

        if (!user.hasRole(Role.ROLE_ADMIN)) {
            Long shopOwnerId = resolveShopOwnerId(user);
            fournisseurs = fournisseurs.stream()
                    .filter(f -> f.getUser() != null
                            && f.getUser().getId().equals(shopOwnerId))
                    .toList();
        }

        return fournisseurs.stream()
                .map(fournisseurMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // CREATE — USER / SELLER uniquement
    // -----------------------------------------

    @Transactional
    public FournisseurResponseDTO create(FournisseurRequestDTO dto, String userEmail) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            throw new BusinessException(
                    "Un ADMIN ne crée pas de fournisseurs. Créez un USER pour cela.");
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        User shopOwner = userRepository.findById(shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("User (boutique)", shopOwnerId));

        if (fournisseurRepository.existsByCodeAndEntrepriseIdAndUserId(
                dto.getCode(), entreprise.getId(), shopOwnerId)) {
            throw new BusinessException(
                    "Vous avez déjà un fournisseur avec le code " + dto.getCode());
        }
        if (fournisseurRepository.existsByEmailAndEntrepriseIdAndUserId(
                dto.getEmail(), entreprise.getId(), shopOwnerId)) {
            throw new BusinessException(
                    "Vous avez déjà un fournisseur avec l'email " + dto.getEmail());
        }

        Fournisseur fournisseur = fournisseurMapper.toEntity(dto, entreprise, shopOwner);
        Fournisseur saved = fournisseurRepository.save(fournisseur);
        log.info("Fournisseur {} créé par {} pour la boutique de {} (entreprise: {})",
                saved.getCode(), userEmail, shopOwner.getEmail(), entreprise.getNom());
        return fournisseurMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @Transactional
    public FournisseurResponseDTO update(Long id, FournisseurRequestDTO dto) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Fournisseur existing = findMyFournisseur(user, entreprise, id);

        Long shopOwnerId = existing.getUser() != null ? existing.getUser().getId() : null;
        if (shopOwnerId != null) {
            if (!existing.getCode().equals(dto.getCode())
                    && fournisseurRepository.existsByCodeAndEntrepriseIdAndUserId(
                    dto.getCode(), entreprise.getId(), shopOwnerId)) {
                throw new BusinessException(
                        "Vous avez déjà un fournisseur avec le code " + dto.getCode());
            }
            if (!existing.getEmail().equals(dto.getEmail())
                    && fournisseurRepository.existsByEmailAndEntrepriseIdAndUserId(
                    dto.getEmail(), entreprise.getId(), shopOwnerId)) {
                throw new BusinessException(
                        "Vous avez déjà un fournisseur avec l'email " + dto.getEmail());
            }
        }

        fournisseurMapper.updateEntity(existing, dto);
        Fournisseur updated = fournisseurRepository.save(existing);
        return fournisseurMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // DELETE / DEACTIVATE
    // -----------------------------------------

    @Transactional
    public void delete(Long id) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Fournisseur fournisseur = findMyFournisseur(user, entreprise, id);

        long nbCommandes = commandeAchatRepository.countByFournisseurId(id);
        if (nbCommandes > 0) {
            throw new BusinessException(
                    "Impossible de supprimer ce fournisseur : " + nbCommandes
                            + " commande(s) d'achat existent. Désactivez-le à la place.");
        }

        fournisseurRepository.delete(fournisseur);
        log.info("Fournisseur {} supprimé", fournisseur.getCode());
    }

    @Transactional
    public void deactivate(Long id) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Fournisseur fournisseur = findMyFournisseur(user, entreprise, id);
        fournisseur.setActif(false);
        fournisseurRepository.save(fournisseur);
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    private Long resolveShopOwnerId(User user) {
        if (user.hasRole(Role.ROLE_SELLER)) {
            if (user.getPatron() == null) {
                throw new BusinessException("Ce SELLER n'a pas de patron assigné");
            }
            return user.getPatron().getId();
        }
        return user.getId();
    }

    private Fournisseur findMyFournisseur(User user, Entreprise entreprise, Long id) {
        if (user.hasRole(Role.ROLE_ADMIN)) {
            return fournisseurRepository
                    .findByIdAndEntrepriseIdWithDetails(id, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id));
        }
        Long shopOwnerId = resolveShopOwnerId(user);
        return fournisseurRepository
                .findByIdAndEntrepriseIdAndUserIdWithDetails(
                        id, entreprise.getId(), shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id));
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