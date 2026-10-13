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

    public EntrepriseResponseDTO findMyEntreprise() {
        User user = getCurrentUser();
        if (user.getEntreprise() == null) {
            throw new ResourceNotFoundException(
                    "Aucune entreprise associée. Contactez votre administrateur.");
        }
        return entrepriseMapper.toResponseDTO(user.getEntreprise());
    }

    public EntrepriseResponseDTO findById(Long id) {
        User user = getCurrentUser();
        Entreprise entreprise = entrepriseRepository.findByIdWithOwner(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entreprise", id));

        // SUPER_ADMIN voit tout
        if (user.hasRole(Role.ROLE_SUPER_ADMIN)) {
            return entrepriseMapper.toResponseDTO(entreprise);
        }

        // Les autres : doivent être dans la même entreprise
        if (user.getEntreprise() == null
                || !user.getEntreprise().getId().equals(id)) {
            throw new ResourceNotFoundException("Entreprise", id);
        }
        return entrepriseMapper.toResponseDTO(entreprise);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @Transactional
    public EntrepriseResponseDTO update(Long id, EntrepriseRequestDTO dto) {
        User user = getCurrentUser();
        Entreprise existing = entrepriseRepository.findByIdWithOwner(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entreprise", id));

        // Seul l'ADMIN (owner) peut modifier
        if (!user.hasRole(Role.ROLE_ADMIN)) {
            throw new BusinessException("Seul l'ADMIN peut modifier l'entreprise");
        }
        if (user.getEntreprise() == null || !user.getEntreprise().getId().equals(id)) {
            throw new ResourceNotFoundException("Entreprise", id);
        }

        if (!existing.getNom().equals(dto.getNom())) {
            existing.setSlug(rendreUniqueSlug(genererSlug(dto.getNom())));
        }

        entrepriseMapper.updateEntity(existing, dto);
        return entrepriseMapper.toResponseDTO(entrepriseRepository.save(existing));
    }

    // -----------------------------------------
    // LOGO
    // -----------------------------------------

    @Transactional
    public EntrepriseResponseDTO uploadLogo(MultipartFile file) {
        Entreprise entreprise = getMyEntrepriseEntity();

        if (entreprise.getLogo() != null && !entreprise.getLogo().isBlank()) {
            fileStorageService.delete(entreprise.getLogo(), SOUS_DOSSIER);
        }

        String nomFichier = fileStorageService.store(file, SOUS_DOSSIER);
        entreprise.setLogo(nomFichier);

        return entrepriseMapper.toResponseDTO(entrepriseRepository.save(entreprise));
    }

    @Transactional
    public EntrepriseResponseDTO deleteLogo() {
        Entreprise entreprise = getMyEntrepriseEntity();
        if (entreprise.getLogo() == null) {
            throw new BusinessException("Cette entreprise n'a pas de logo");
        }
        fileStorageService.delete(entreprise.getLogo(), SOUS_DOSSIER);
        entreprise.setLogo(null);
        return entrepriseMapper.toResponseDTO(entrepriseRepository.save(entreprise));
    }

    // -----------------------------------------
    // DELETE — SUPER_ADMIN uniquement
    // -----------------------------------------

    @Transactional
    public void delete(Long id) {
        Entreprise entreprise = entrepriseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entreprise", id));
        if (entreprise.getLogo() != null) {
            fileStorageService.delete(entreprise.getLogo(), SOUS_DOSSIER);
        }
        entrepriseRepository.delete(entreprise);
        log.info("Entreprise {} supprimée", entreprise.getNom());
    }

    // -----------------------------------------
    // PRIVÉ
    // -----------------------------------------

    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null) throw new BusinessException("Utilisateur non authentifié");
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User introuvable avec l'email : " + email));
    }

    private Entreprise getMyEntrepriseEntity() {
        User user = getCurrentUser();
        if (user.getEntreprise() == null) {
            throw new ResourceNotFoundException(
                    "Aucune entreprise associée. Contactez votre administrateur.");
        }
        return user.getEntreprise();
    }

    private String genererSlug(String nom) {
        if (nom == null) return "entreprise";
        String n = Normalizer.normalize(nom, Normalizer.Form.NFD);
        return n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase().trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    private String rendreUniqueSlug(String slug) {
        if (!entrepriseRepository.existsBySlug(slug)) return slug;
        int i = 1;
        String base = slug;
        while (entrepriseRepository.existsBySlug(slug)) {
            slug = base + "-" + i++;
        }
        return slug;
    }
}