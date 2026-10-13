package com.innovatech.api.service;

import com.innovatech.api.dto.UserResponseDTO;
import com.innovatech.api.entity.Entreprise;
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
    // READ (SUPER_ADMIN uniquement)
    // -----------------------------------------

    public List<UserResponseDTO> findAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponseDTO)
                .toList();
    }

    public UserResponseDTO findUserById(Long id) {
        User user = userRepository.findByIdWithPatron(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return userMapper.toResponseDTO(user);
    }

    public boolean superAdminExists() {
        return userRepository.findAll().stream()
                .anyMatch(u -> u.getRoles().contains(Role.ROLE_SUPER_ADMIN));
    }

    // -----------------------------------------
    // GESTION DES RÔLES (SUPER_ADMIN)
    // -----------------------------------------

    @Transactional
    public UserResponseDTO promoteToSuperAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.getRoles().contains(Role.ROLE_SUPER_ADMIN)) {
            throw new BusinessException("Cet utilisateur est déjà SUPER_ADMIN");
        }

        Set<Role> roles = new HashSet<>(user.getRoles());
        roles.add(Role.ROLE_SUPER_ADMIN);
        user.setRoles(roles);

        User updated = userRepository.save(user);
        log.info("Utilisateur {} promu SUPER_ADMIN", user.getEmail());
        return userMapper.toResponseDTO(updated);
    }

    @Transactional
    public UserResponseDTO demoteFromSuperAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!user.getRoles().contains(Role.ROLE_SUPER_ADMIN)) {
            throw new BusinessException("Cet utilisateur n'est pas SUPER_ADMIN");
        }

        Set<Role> roles = new HashSet<>(user.getRoles());
        roles.remove(Role.ROLE_SUPER_ADMIN);
        user.setRoles(roles);

        User updated = userRepository.save(user);
        return userMapper.toResponseDTO(updated);
    }

    @Transactional
    public UserResponseDTO toggleActif(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setActif(!user.isActif());
        return userMapper.toResponseDTO(userRepository.save(user));
    }

    // -----------------------------------------
    // HIÉRARCHIE : SELLER → USER (patron)
    // -----------------------------------------

    @Transactional
    public UserResponseDTO assignPatron(Long sellerId, Long patronId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("User (SELLER)", sellerId));
        User patron = userRepository.findById(patronId)
                .orElseThrow(() -> new ResourceNotFoundException("User (patron)", patronId));

        if (!seller.getRoles().contains(Role.ROLE_SELLER)) {
            throw new BusinessException("L'utilisateur n'est pas SELLER");
        }
        if (patron.getRoles().contains(Role.ROLE_SELLER)) {
            throw new BusinessException("Un SELLER ne peut pas être patron");
        }
        if (seller.getId().equals(patron.getId())) {
            throw new BusinessException("Un utilisateur ne peut pas être son propre patron");
        }

        seller.setPatron(patron);
        // ⚠️ Le SELLER hérite de l'entreprise de son patron
        seller.setEntreprise(patron.getEntreprise());

        User updated = userRepository.save(seller);
        log.info("SELLER {} rattaché au patron {}", seller.getEmail(), patron.getEmail());
        return userMapper.toResponseDTO(updated);
    }

    @Transactional
    public UserResponseDTO removePatron(Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("User (SELLER)", sellerId));

        if (seller.getPatron() == null) {
            throw new BusinessException("Cet utilisateur n'a pas de patron");
        }

        seller.setPatron(null);
        seller.setEntreprise(null);

        return userMapper.toResponseDTO(userRepository.save(seller));
    }

    public List<UserResponseDTO> findEmployes(Long patronId) {
        if (!userRepository.existsById(patronId)) {
            throw new ResourceNotFoundException("User (patron)", patronId);
        }
        return userRepository.findByPatronId(patronId).stream()
                .map(userMapper::toResponseDTO)
                .toList();
    }
}