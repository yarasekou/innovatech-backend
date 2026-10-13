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
 * Service de gestion des USERs (managers) par un ADMIN.
 * <p>
 * Règles métier :
 * <ul>
 *   <li>Un ADMIN crée des USERs dans SON entreprise</li>
 *   <li>Chaque USER est un silo isolé (sa propre boutique)</li>
 *   <li>Un USER ne voit PAS les autres USERs de la même entreprise</li>
 *   <li>Un ADMIN peut désactiver un USER (jamais supprimer)</li>
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
    // CREATE — ADMIN crée un USER
    // -----------------------------------------

    @Transactional
    public UserResponseDTO createUser(CreateUserDTO dto) {
        User admin = getCurrentUser();

        // Seul un ADMIN peut créer un USER
        if (!admin.hasRole(Role.ROLE_ADMIN)) {
            throw new BusinessException("Seul un ADMIN peut créer des USERs");
        }

        // L'ADMIN doit avoir une entreprise
        if (admin.getEntreprise() == null) {
            throw new BusinessException(
                    "Vous devez avoir une entreprise avant de créer des USERs");
        }

        // Vérifier unicité email
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException(
                    "Un utilisateur avec l'email " + dto.getEmail() + " existe déjà");
        }

        // Vérifier unicité username
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BusinessException(
                    "Le nom d'utilisateur " + dto.getUsername() + " est déjà pris");
        }

        // Créer le USER (rôle unique ROLE_USER)
        // ⚠️ HashSet (mutable), PAS Set.of() → Hibernate a besoin d'une collection mutable
        Set<Role> roles = new HashSet<>();
        roles.add(Role.ROLE_USER);

        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .prenom(dto.getPrenom())
                .nom(dto.getNom())
                .roles(roles)
                .entreprise(admin.getEntreprise())   // ← héritage entreprise
                .actif(true)
                .build();

        User saved = userRepository.save(user);
        log.info("USER {} créé par ADMIN {} (entreprise: {})",
                saved.getEmail(), admin.getEmail(), admin.getEntreprise().getNom());
        return userMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // READ — ADMIN voit SES USERs
    // -----------------------------------------

    public List<UserResponseDTO> findMyUsers() {
        User admin = getCurrentUser();

        if (!admin.hasRole(Role.ROLE_ADMIN)) {
            throw new BusinessException("Seul un ADMIN peut lister ses USERs");
        }

        if (admin.getEntreprise() == null) {
            throw new BusinessException("Vous n'avez pas d'entreprise");
        }

        return userRepository.findByEntrepriseIdAndRole(
                        admin.getEntreprise().getId(), Role.ROLE_USER)
                .stream()
                .map(userMapper::toResponseDTO)
                .toList();
    }

    public UserResponseDTO findMyUserById(Long userId) {
        User admin = getCurrentUser();
        User user = findMyUserEntity(admin, userId);
        return userMapper.toResponseDTO(user);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @Transactional
    public UserResponseDTO updateUser(Long userId, UpdateUserDTO dto) {
        User admin = getCurrentUser();
        User user = findMyUserEntity(admin, userId);

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
        log.info("USER {} modifié par ADMIN {}", user.getEmail(), admin.getEmail());
        return userMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // TOGGLE ACTIF
    // -----------------------------------------

    @Transactional
    public UserResponseDTO toggleActif(Long userId) {
        User admin = getCurrentUser();
        User user = findMyUserEntity(admin, userId);

        user.setActif(!user.isActif());
        User updated = userRepository.save(user);
        log.info("USER {} {} par ADMIN {}",
                user.getEmail(),
                user.isActif() ? "activé" : "désactivé",
                admin.getEmail());
        return userMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    /**
     * Vérifie que l'utilisateur connecté est un ADMIN
     * et que le USER ciblé appartient à SA entreprise.
     */
    private User findMyUserEntity(User admin, Long userId) {
        if (!admin.hasRole(Role.ROLE_ADMIN)) {
            throw new BusinessException("Seul un ADMIN peut gérer ses USERs");
        }

        if (admin.getEntreprise() == null) {
            throw new BusinessException("Vous n'avez pas d'entreprise");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Vérifier appartenance entreprise
        if (user.getEntreprise() == null
                || !user.getEntreprise().getId().equals(admin.getEntreprise().getId())) {
            throw new ResourceNotFoundException("User", userId);
        }

        // Vérifier que c'est bien un USER (pas un ADMIN, ni un SELLER)
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
}