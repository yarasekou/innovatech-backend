package com.innovatech.api.service;

import com.innovatech.api.dto.UserResponseDTO;
import com.innovatech.api.entity.Role;
import com.innovatech.api.entity.User;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.UserMapper;
import com.innovatech.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    // -----------------------------------------
    // READ
    // -----------------------------------------

    public List<UserResponseDTO> findAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponseDTO)
                .toList();
    }

    public UserResponseDTO findUserById(Long id) {
        User user = userRepository.findByIdWithPatron(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return userMapper.toResponseDTO(user);
    }

    /**
     * Vérifie s'il existe au moins un ADMIN.
     */
    public boolean adminExists() {
        return userRepository.findAll()
                .stream()
                .anyMatch(u -> u.getRoles().contains(Role.ROLE_ADMIN));
    }

    // -----------------------------------------
    // GESTION DES RÔLES
    // -----------------------------------------

    @Transactional
    public UserResponseDTO promoteToAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.getRoles().contains(Role.ROLE_ADMIN)) {
            throw new BusinessException("Cet utilisateur est déjà administrateur");
        }

        Set<Role> roles = new HashSet<>(user.getRoles());
        roles.add(Role.ROLE_ADMIN);
        user.setRoles(roles);

        User updated = userRepository.save(user);
        log.info("Utilisateur {} promu ADMIN", user.getEmail());
        return userMapper.toResponseDTO(updated);
    }

    @Transactional
    public UserResponseDTO demoteToUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!user.getRoles().contains(Role.ROLE_ADMIN)) {
            throw new BusinessException("Cet utilisateur n'est pas administrateur");
        }

        Set<Role> roles = new HashSet<>(user.getRoles());
        roles.remove(Role.ROLE_ADMIN);
        roles.add(Role.ROLE_USER);
        user.setRoles(roles);

        User updated = userRepository.save(user);
        log.info("Utilisateur {} rétrogradé USER", user.getEmail());
        return userMapper.toResponseDTO(updated);
    }

    /**
     * Promeut un USER en SELLER (rôle UNIQUE).
     */
    @Transactional
    public UserResponseDTO promoteToSeller(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.getRoles().contains(Role.ROLE_SELLER)) {
            throw new BusinessException("Cet utilisateur est déjà vendeur");
        }

        Set<Role> roles = new HashSet<>();
        roles.add(Role.ROLE_SELLER);
        user.setRoles(roles);

        User updated = userRepository.save(user);
        log.info("Utilisateur {} promu SELLER (rôle unique)", user.getEmail());
        return userMapper.toResponseDTO(updated);
    }

    /**
     * Rétrograde un SELLER en USER.
     */
    @Transactional
    public UserResponseDTO demoteFromSeller(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!user.getRoles().contains(Role.ROLE_SELLER)) {
            throw new BusinessException("Cet utilisateur n'est pas vendeur");
        }

        Set<Role> roles = new HashSet<>();
        roles.add(Role.ROLE_USER);
        user.setRoles(roles);

        User updated = userRepository.save(user);
        log.info("Utilisateur {} rétrogradé USER (plus SELLER)", user.getEmail());
        return userMapper.toResponseDTO(updated);
    }

    @Transactional
    public UserResponseDTO toggleActif(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setActif(!user.isActif());
        User updated = userRepository.save(user);
        return userMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // HIÉRARCHIE : SELLER → PATRON (USER)
    // -----------------------------------------

    /**
     * Rattache un SELLER à un patron (USER).
     */
    @Transactional
    public UserResponseDTO assignPatron(Long sellerId, Long patronId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("User (SELLER)", sellerId));

        User patron = userRepository.findById(patronId)
                .orElseThrow(() -> new ResourceNotFoundException("User (patron)", patronId));

        // Le SELLER doit avoir le rôle SELLER
        if (!seller.getRoles().contains(Role.ROLE_SELLER)) {
            throw new BusinessException(
                    "L'utilisateur " + seller.getEmail() + " n'est pas SELLER");
        }

        // Le patron ne peut PAS être SELLER (pas de chaînes)
        if (patron.getRoles().contains(Role.ROLE_SELLER)) {
            throw new BusinessException("Un SELLER ne peut pas être patron");
        }

        // Le patron ne peut PAS être ADMIN
        if (patron.getRoles().contains(Role.ROLE_ADMIN)) {
            throw new BusinessException("Un ADMIN ne peut pas être patron d'un SELLER");
        }

        // Pas d'auto-patron
        if (seller.getId().equals(patron.getId())) {
            throw new BusinessException("Un utilisateur ne peut pas être son propre patron");
        }

        seller.setPatron(patron);
        User updated = userRepository.save(seller);
        log.info("SELLER {} rattaché au patron {}", seller.getEmail(), patron.getEmail());
        return userMapper.toResponseDTO(updated);
    }

    /**
     * Retire le patron d'un SELLER.
     */
    @Transactional
    public UserResponseDTO removePatron(Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("User (SELLER)", sellerId));

        if (seller.getPatron() == null) {
            throw new BusinessException("Cet utilisateur n'a pas de patron");
        }

        seller.setPatron(null);
        User updated = userRepository.save(seller);
        log.info("Patron retiré pour {}", seller.getEmail());
        return userMapper.toResponseDTO(updated);
    }

    /**
     * Liste les SELLERs (employés) d'un patron.
     */
    public List<UserResponseDTO> findEmployes(Long patronId) {
        if (!userRepository.existsById(patronId)) {
            throw new ResourceNotFoundException("User (patron)", patronId);
        }

        return userRepository.findByPatronId(patronId)
                .stream()
                .map(userMapper::toResponseDTO)
                .toList();
    }
}