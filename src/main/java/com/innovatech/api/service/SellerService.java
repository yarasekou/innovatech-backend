package com.innovatech.api.service;

import com.innovatech.api.dto.CreateSellerDTO;
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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SellerService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // -----------------------------------------
    // CREATE — Un USER crée un SELLER
    // -----------------------------------------

    @Transactional
    public UserResponseDTO createSeller(CreateSellerDTO dto) {
        User patron = getCurrentUser();

        if (!patron.hasRole(Role.ROLE_USER)) {
            throw new BusinessException("Seul un USER (manager) peut créer un SELLER");
        }

        if (patron.getEntreprise() == null) {
            throw new BusinessException(
                    "Vous devez être rattaché à une entreprise avant de créer des SELLERs");
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException(
                    "Un utilisateur avec l'email " + dto.getEmail() + " existe déjà");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BusinessException(
                    "Le nom d'utilisateur " + dto.getUsername() + " est déjà pris");
        }

        // ⚠️ HashSet (mutable), PAS Set.of()
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
                .entreprise(patron.getEntreprise())
                .actif(true)
                .build();

        User saved = userRepository.save(seller);
        log.info("SELLER {} créé par {} (entreprise: {})",
                saved.getEmail(), patron.getEmail(), patron.getEntreprise().getNom());
        return userMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // READ
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

        if (seller.getPatron() == null
                || !seller.getPatron().getId().equals(patron.getId())) {
            throw new ResourceNotFoundException("Seller", sellerId);
        }

        return userMapper.toResponseDTO(seller);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @Transactional
    public UserResponseDTO toggleSellerActif(Long sellerId) {
        User patron = getCurrentUser();
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller", sellerId));

        if (seller.getPatron() == null
                || !seller.getPatron().getId().equals(patron.getId())) {
            throw new ResourceNotFoundException("Seller", sellerId);
        }

        seller.setActif(!seller.isActif());
        User updated = userRepository.save(seller);
        log.info("SELLER {} {}", seller.getEmail(), seller.isActif() ? "activé" : "désactivé");
        return userMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------

    @Transactional
    public void deleteSeller(Long sellerId) {
        User patron = getCurrentUser();
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller", sellerId));

        if (seller.getPatron() == null
                || !seller.getPatron().getId().equals(patron.getId())) {
            throw new ResourceNotFoundException("Seller", sellerId);
        }

        userRepository.delete(seller);
        log.info("SELLER {} supprimé par {}", seller.getEmail(), patron.getEmail());
    }

    // -----------------------------------------
    // MÉTHODE PRIVÉE
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
}