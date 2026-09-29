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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Liste tous les utilisateurs.
     */
    public List<UserResponseDTO> findAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponseDTO)
                .toList();
    }

    /**
     * Détail d'un utilisateur.
     */
    public UserResponseDTO findUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return userMapper.toResponseDTO(user);
    }

    /**
     * Promeut un utilisateur en ADMIN.
     */
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

    /**
     * Rétrograde un ADMIN en USER.
     */
    @Transactional
    public UserResponseDTO demoteToUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!user.getRoles().contains(Role.ROLE_ADMIN)) {
            throw new BusinessException("Cet utilisateur n'est pas administrateur");
        }

        Set<Role> roles = new HashSet<>(user.getRoles());
        roles.remove(Role.ROLE_ADMIN);
        // S'assurer qu'il garde au moins ROLE_USER
        roles.add(Role.ROLE_USER);
        user.setRoles(roles);

        User updated = userRepository.save(user);
        log.info("Utilisateur {} rétrogradé USER", user.getEmail());
        return userMapper.toResponseDTO(updated);
    }

    /**
     * Active/désactive un utilisateur.
     */
    @Transactional
    public UserResponseDTO toggleActif(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setActif(!user.isActif());
        User updated = userRepository.save(user);
        return userMapper.toResponseDTO(updated);
    }

    /**
     * Vérifie s'il existe au moins un ADMIN.
     */
    public boolean adminExists() {
        return userRepository.findAll()
                .stream()
                .anyMatch(u -> u.getRoles().contains(Role.ROLE_ADMIN));
    }

    @Transactional
    public UserResponseDTO promoteToSeller(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.getRoles().contains(Role.ROLE_SELLER)) {
            throw new BusinessException("Cet utilisateur est déjà vendeur");
        }

        Set<Role> roles = new HashSet<>(user.getRoles());
        roles.add(Role.ROLE_SELLER);
        user.setRoles(roles);

        User updated = userRepository.save(user);
        log.info("Utilisateur {} promu SELLER", user.getEmail());
        return userMapper.toResponseDTO(updated);
    }

    @Transactional
    public UserResponseDTO demoteFromSeller(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!user.getRoles().contains(Role.ROLE_SELLER)) {
            throw new BusinessException("Cet utilisateur n'est pas vendeur");
        }

        Set<Role> roles = new HashSet<>(user.getRoles());
        roles.remove(Role.ROLE_SELLER);
        roles.add(Role.ROLE_USER);   // Garder au moins ROLE_USER
        user.setRoles(roles);

        User updated = userRepository.save(user);
        log.info("Utilisateur {} rétrogradé (plus SELLER)", user.getEmail());
        return userMapper.toResponseDTO(updated);
    }
}