package com.innovatech.api.service;

import com.innovatech.api.dto.FournisseurRequestDTO;
import com.innovatech.api.dto.FournisseurResponseDTO;
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
        if (SecurityUtils.isAdmin()) {
            return fournisseurRepository.findAllWithUser()
                    .stream()
                    .map(fournisseurMapper::toResponseDTO)
                    .toList();
        }

        Long dataOwnerId = getCurrentDataOwnerId();
        return fournisseurRepository.findByUserIdWithUser(dataOwnerId)
                .stream()
                .map(fournisseurMapper::toResponseDTO)
                .toList();
    }

    public List<FournisseurResponseDTO> findAllActifs() {
        if (SecurityUtils.isAdmin()) {
            return fournisseurRepository.findAllWithUser()
                    .stream()
                    .filter(Fournisseur::isActif)
                    .map(fournisseurMapper::toResponseDTO)
                    .toList();
        }

        Long dataOwnerId = getCurrentDataOwnerId();
        return fournisseurRepository.findByUserIdAndActifTrueWithUser(dataOwnerId)
                .stream()
                .map(fournisseurMapper::toResponseDTO)
                .toList();
    }

    public FournisseurResponseDTO findById(Long id) {
        Fournisseur fournisseur;

        if (SecurityUtils.isAdmin()) {
            fournisseur = fournisseurRepository.findByIdWithUser(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id));
        } else {
            Long dataOwnerId = getCurrentDataOwnerId();
            fournisseur = fournisseurRepository.findByIdAndUserIdWithUser(id, dataOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id));
        }

        return fournisseurMapper.toResponseDTO(fournisseur);
    }

    public FournisseurResponseDTO findByCode(String code) {
        // Pour un user : chercher par code ET user
        if (!SecurityUtils.isAdmin()) {
            Long dataOwnerId = getCurrentDataOwnerId();
            Fournisseur fournisseur = fournisseurRepository.findByCodeAndUserId(code, dataOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Fournisseur avec code " + code + " introuvable"));
            return fournisseurMapper.toResponseDTO(fournisseur);
        }

        // Admin : chercher par code (peut être multiple, mais on prend le 1er)
        // TODO : adapter si besoin
        throw new BusinessException("Utilisez l'ID pour rechercher un fournisseur");
    }

    public List<FournisseurResponseDTO> search(String query) {
        if (SecurityUtils.isAdmin()) {
            return fournisseurRepository.findAllWithUser()
                    .stream()
                    .filter(f -> f.getNom().toLowerCase().contains(query.toLowerCase()))
                    .map(fournisseurMapper::toResponseDTO)
                    .toList();
        }

        Long dataOwnerId = getCurrentDataOwnerId();
        return fournisseurRepository.searchByUserId(dataOwnerId, query)
                .stream()
                .map(fournisseurMapper::toResponseDTO)
                .toList();
    }

    public List<FournisseurResponseDTO> findByPays(String pays) {
        if (SecurityUtils.isAdmin()) {
            return fournisseurRepository.findAllWithUser()
                    .stream()
                    .filter(f -> pays.equalsIgnoreCase(f.getPays()))
                    .map(fournisseurMapper::toResponseDTO)
                    .toList();
        }

        Long dataOwnerId = getCurrentDataOwnerId();
        return fournisseurRepository.findByUserIdAndPays(dataOwnerId, pays)
                .stream()
                .map(fournisseurMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @Transactional
    public FournisseurResponseDTO create(FournisseurRequestDTO dto, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User introuvable avec l'email : " + userEmail));

        Long ownerId = user.getId();

        if (fournisseurRepository.existsByCodeAndUserId(dto.getCode(), ownerId)) {
            throw new BusinessException(
                    "Vous avez déjà un fournisseur avec le code " + dto.getCode());
        }
        if (fournisseurRepository.existsByEmailAndUserId(dto.getEmail(), ownerId)) {
            throw new BusinessException(
                    "Vous avez déjà un fournisseur avec l'email " + dto.getEmail());
        }

        Fournisseur fournisseur = fournisseurMapper.toEntity(dto, user);
        Fournisseur saved = fournisseurRepository.save(fournisseur);
        log.info("Fournisseur {} créé par {}", saved.getCode(), userEmail);
        return fournisseurMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @Transactional
    public FournisseurResponseDTO update(Long id, FournisseurRequestDTO dto) {
        Fournisseur existing;

        if (SecurityUtils.isAdmin()) {
            existing = fournisseurRepository.findByIdWithUser(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id));
        } else {
            Long dataOwnerId = getCurrentDataOwnerId();
            existing = fournisseurRepository.findByIdAndUserIdWithUser(id, dataOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id));
        }

        Long ownerId = existing.getUser() != null ? existing.getUser().getId() : null;

        // Vérifier unicité si changé
        if (!existing.getCode().equals(dto.getCode())
                && ownerId != null
                && fournisseurRepository.existsByCodeAndUserId(dto.getCode(), ownerId)) {
            throw new BusinessException(
                    "Vous avez déjà un fournisseur avec le code " + dto.getCode());
        }

        if (!existing.getEmail().equals(dto.getEmail())
                && ownerId != null
                && fournisseurRepository.existsByEmailAndUserId(dto.getEmail(), ownerId)) {
            throw new BusinessException(
                    "Vous avez déjà un fournisseur avec l'email " + dto.getEmail());
        }

        fournisseurMapper.updateEntity(existing, dto);
        Fournisseur updated = fournisseurRepository.save(existing);
        return fournisseurMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------

    @Transactional
    public void delete(Long id) {
        Fournisseur fournisseur;

        if (SecurityUtils.isAdmin()) {
            fournisseur = fournisseurRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id));
        } else {
            Long dataOwnerId = getCurrentDataOwnerId();
            fournisseur = fournisseurRepository.findByIdAndUserIdWithUser(id, dataOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id));
        }

        long nbCommandes = commandeAchatRepository.countByFournisseurId(id);
        if (nbCommandes > 0) {
            throw new BusinessException(
                    "Impossible de supprimer ce fournisseur : " + nbCommandes
                            + " commande(s) d'achat existent. Désactivez-le à la place.");
        }

        fournisseurRepository.delete(fournisseur);
    }

    @Transactional
    public void deactivate(Long id) {
        Fournisseur fournisseur;

        if (SecurityUtils.isAdmin()) {
            fournisseur = fournisseurRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id));
        } else {
            Long dataOwnerId = getCurrentDataOwnerId();
            fournisseur = fournisseurRepository.findByIdAndUserIdWithUser(id, dataOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id));
        }

        fournisseur.setActif(false);
        fournisseurRepository.save(fournisseur);
    }

    // -----------------------------------------
    // MÉTHODE PRIVÉE
    // -----------------------------------------

    private Long getCurrentDataOwnerId() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null) {
            throw new BusinessException("Utilisateur non authentifié");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User introuvable avec l'email : " + email));

        if (user.getRoles().contains(Role.ROLE_SELLER)) {
            if (user.getPatron() == null) {
                throw new BusinessException("Ce SELLER n'a pas de patron assigné");
            }
            return user.getPatron().getId();
        }

        return user.getId();
    }
}