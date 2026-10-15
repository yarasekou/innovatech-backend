package com.innovatech.api.service;

import com.innovatech.api.dto.CreateUserDTO;
import com.innovatech.api.dto.UpdateUserDTO;
import com.innovatech.api.dto.UserResponseDTO;
import com.innovatech.api.entity.Role;
import com.innovatech.api.entity.User;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.UserMapper;
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
 * Service de gestion des USERs (managers).
 * <p>
 * Règles :
 * <ul>
 *   <li>ADMIN       → crée et gère SES USERs (dans son entreprise)</li>
 *   <li>SUPER_ADMIN → voit et gère TOUS les USERs (supervision plateforme)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // -----------------------------------------
    // CREATE — ADMIN uniquement
    // -----------------------------------------

    @Transactional
    public UserResponseDTO createUser(CreateUserDTO dto) {
        User currentUser = getCurrentUser();

        // Seul un ADMIN peut créer un USER
        if (!currentUser.hasRole(Role.ROLE_ADMIN)) {
            throw new BusinessException(
                    "Seul un ADMIN peut créer des USERs. Créez une entreprise d'abord.");
        }

        if (currentUser.getEntreprise() == null) {
            throw new BusinessException(
                    "Vous devez avoir une entreprise avant de créer des USERs");
        }

        // Vérifier unicité
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException(
                    "Un utilisateur avec l'email " + dto.getEmail() + " existe déjà");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BusinessException(
                    "Le nom d'utilisateur " + dto.getUsername() + " est déjà pris");
        }

        Set<Role> roles = new HashSet<>();
        roles.add(Role.ROLE_USER);

        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .prenom(dto.getPrenom())
                .nom(dto.getNom())
                .roles(roles)
                .entreprise(currentUser.getEntreprise())
                .actif(true)
                .build();

        User saved = userRepository.save(user);
        log.info("USER {} créé par {} (entreprise: {})",
                saved.getEmail(), currentUser.getEmail(),
                currentUser.getEntreprise().getNom());
        return userMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // READ — ADMIN voit les siens, SUPER_ADMIN voit tout
    // -----------------------------------------

    public List<UserResponseDTO> findMyUsers() {
        User currentUser = getCurrentUser();

        // -----------------------------------------
        // SUPER_ADMIN → ADMINs + USERs (sans lui, sans SELLERs)
        // -----------------------------------------
        if (currentUser.hasRole(Role.ROLE_SUPER_ADMIN)) {
            return userRepository.findAllWithEntreprise()
                    .stream()
                    .filter(u -> !u.getId().equals(currentUser.getId()))           // sans lui
                    .filter(u -> !u.hasRole(Role.ROLE_SELLER))                     // sans SELLERs
                    .map(userMapper::toResponseDTO)
                    .toList();
        }

        // -----------------------------------------
        // ADMIN → sa ligne + ses USERs (sans SELLERs)
        // -----------------------------------------
        if (currentUser.hasRole(Role.ROLE_ADMIN)) {
            if (currentUser.getEntreprise() == null) {
                throw new BusinessException("Vous n'avez pas d'entreprise");
            }

            java.util.List<User> result = new java.util.ArrayList<>();

            // 1. L'ADMIN lui-même
            result.add(currentUser);

            // 2. Ses USERs (findByEntrepriseIdAndRole filtre déjà sur ROLE_USER)
            result.addAll(userRepository.findByEntrepriseIdAndRole(
                    currentUser.getEntreprise().getId(), Role.ROLE_USER));

            return result.stream()
                    .map(userMapper::toResponseDTO)
                    .toList();
        }

        // -----------------------------------------
        // USER / SELLER → refusé
        // -----------------------------------------
        throw new BusinessException("Accès refusé");
    }

    public UserResponseDTO findMyUserById(Long userId) {
        User currentUser = getCurrentUser();
        User user = findAccessibleUser(currentUser, userId);
        return userMapper.toResponseDTO(user);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @Transactional
    public UserResponseDTO updateUser(Long userId, UpdateUserDTO dto) {
        User currentUser = getCurrentUser();
        User user = findAccessibleUser(currentUser, userId);

        if (dto.getPrenom() != null) {
            user.setPrenom(dto.getPrenom());
        }
        if (dto.getNom() != null) {
            user.setNom(dto.getNom());
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        User updated = userRepository.save(user);
        log.info("USER {} modifié par {}", user.getEmail(), currentUser.getEmail());
        return userMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // TOGGLE ACTIF
    // -----------------------------------------

    @Transactional
    public UserResponseDTO toggleActif(Long userId) {
        User currentUser = getCurrentUser();
        User user = findAccessibleUser(currentUser, userId);

        user.setActif(!user.isActif());
        User updated = userRepository.save(user);
        log.info("USER {} {} par {}",
                user.getEmail(),
                user.isActif() ? "activé" : "désactivé",
                currentUser.getEmail());
        return userMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    /**
     * Récupère un USER en vérifiant les droits d'accès.
     * <ul>
     *   <li>ADMIN → doit être dans SON entreprise</li>
     *   <li>SUPER_ADMIN → peut accéder à n'importe quel USER</li>
     * </ul>
     */
    private User findAccessibleUser(User currentUser, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Refuser les utilisateurs déjà supprimés
        if (user.isSupprime()) {
            throw new ResourceNotFoundException("User", userId);
        }

        // SUPER_ADMIN → accès total
        if (currentUser.hasRole(Role.ROLE_SUPER_ADMIN)) {
            // Vérifier que c'est bien un USER (pas un ADMIN/SUPER_ADMIN)
            if (!user.hasRole(Role.ROLE_USER)) {
                throw new ResourceNotFoundException("User", userId);
            }
            return user;
        }

        // ADMIN → vérifier l'appartenance
        if (!currentUser.hasRole(Role.ROLE_ADMIN)) {
            throw new BusinessException("Accès refusé");
        }

        if (currentUser.getEntreprise() == null
                || user.getEntreprise() == null
                || !user.getEntreprise().getId().equals(currentUser.getEntreprise().getId())) {
            throw new ResourceNotFoundException("User", userId);
        }

        if (!user.hasRole(Role.ROLE_USER)) {
            throw new ResourceNotFoundException("User", userId);
        }

        return user;
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

    @Transactional
    public void deleteUser(Long userId) {
        User currentUser = getCurrentUser();
        User user = findAccessibleUser(currentUser, userId);

        // Vérifier qu'il ne se supprime pas lui-même
        if (user.getId().equals(currentUser.getId())) {
            throw new BusinessException(
                    "Vous ne pouvez pas supprimer votre propre compte");
        }

        // Vérifier qu'il n'est pas SUPER_ADMIN
        if (user.hasRole(Role.ROLE_SUPER_ADMIN)) {
            throw new BusinessException(
                    "Impossible de supprimer un SUPER_ADMIN");
        }

        // Soft delete
        user.setSupprime(true);
        user.setActif(false);
        userRepository.save(user);

        log.info("USER {} supprimé (soft) par {}", user.getEmail(), currentUser.getEmail());
    }
}