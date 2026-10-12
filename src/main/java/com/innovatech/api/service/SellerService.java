package com.innovatech.api.service;

import com.innovatech.api.dto.CreateSellerDTO;
import com.innovatech.api.dto.UserResponseDTO;
import com.innovatech.api.entity.Role;
import com.innovatech.api.entity.User;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.UserMapper;
import com.innovatech.api.repository.EntrepriseRepository;
import com.innovatech.api.repository.UserRepository;
import com.innovatech.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service de gestion des SELLERs par un USER.
 * <p>
 * - Un USER peut créer/supprimer SES SELLERs
 * - Un SELLER ne voit que son profil
 * - L'ADMIN ne peut pas utiliser ce service (utiliser /admin)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SellerService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EntrepriseRepository entrepriseRepository;

    // -----------------------------------------
    // CREATE — Un USER crée un SELLER
    // -----------------------------------------

    @Transactional
    public UserResponseDTO createSeller(CreateSellerDTO dto) {
        User patron = getCurrentUser();

        // ⚠️ Le patron doit avoir une entreprise
        if (!entrepriseRepository.existsByUserId(patron.getId())) {
            throw new BusinessException(
                    "Vous devez créer votre entreprise avant de pouvoir ajouter des SELLERs");
        }

        // Vérifier que le créateur est bien un USER (pas SELLER, pas ADMIN)
        if (patron.getRoles().contains(Role.ROLE_SELLER)) {
            throw new BusinessException("Un SELLER ne peut pas créer d'autres SELLERs");
        }
        if (patron.getRoles().contains(Role.ROLE_ADMIN)) {
            throw new BusinessException("Un ADMIN ne peut pas créer de SELLER via ce endpoint. Utilisez /admin");
        }

        // Vérifier unicité
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("Un utilisateur avec l'email " + dto.getEmail() + " existe déjà");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BusinessException("Le nom d'utilisateur " + dto.getUsername() + " est déjà pris");
        }

        // Créer le SELLER
        Set<Role> roles = new HashSet<>();
        roles.add(Role.ROLE_SELLER);

        User seller = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .prenom(dto.getPrenom())
                .nom(dto.getNom())
                .roles(roles)
                .patron(patron)
                .actif(true)
                .build();

        User saved = userRepository.save(seller);
        log.info("SELLER {} créé par le USER {}", saved.getEmail(), patron.getEmail());
        return userMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // READ — Un USER voit ses SELLERs
    // -----------------------------------------

    public List<UserResponseDTO> findMySellers() {
        User patron = getCurrentUser();

        return userRepository.findByPatronId(patron.getId())
                .stream()
                .map(userMapper::toResponseDTO)
                .toList();
    }

    public UserResponseDTO findMySellerById(Long sellerId) {
        User patron = getCurrentUser();

        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller", sellerId));

        // Vérifier que le SELLER appartient bien à ce patron
        if (seller.getPatron() == null || !seller.getPatron().getId().equals(patron.getId())) {
            throw new ResourceNotFoundException("Seller", sellerId);
        }

        return userMapper.toResponseDTO(seller);
    }

    // -----------------------------------------
    // DELETE — Un USER supprime son SELLER
    // -----------------------------------------

    @Transactional
    public void deleteSeller(Long sellerId) {
        User patron = getCurrentUser();

        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller", sellerId));

        // Vérifier que le SELLER appartient bien à ce patron
        if (seller.getPatron() == null || !seller.getPatron().getId().equals(patron.getId())) {
            throw new ResourceNotFoundException("Seller", sellerId);
        }

        // Supprimer définitivement
        userRepository.delete(seller);
        log.info("SELLER {} supprimé par {}", seller.getEmail(), patron.getEmail());
    }

    // -----------------------------------------
    // TOGGLE ACTIF — Un USER désactive son SELLER
    // -----------------------------------------

    @Transactional
    public UserResponseDTO toggleSellerActif(Long sellerId) {
        User patron = getCurrentUser();

        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller", sellerId));

        if (seller.getPatron() == null || !seller.getPatron().getId().equals(patron.getId())) {
            throw new ResourceNotFoundException("Seller", sellerId);
        }

        seller.setActif(!seller.isActif());
        User updated = userRepository.save(seller);
        log.info("SELLER {} {}", seller.getEmail(), seller.isActif() ? "activé" : "désactivé");
        return userMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // MÉTHODE PRIVÉE
    // -----------------------------------------

    /**
     * Récupère le USER actuellement connecté.
     */
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