package com.innovatech.api.service;

import com.innovatech.api.dto.*;
import com.innovatech.api.entity.Entreprise;
import com.innovatech.api.entity.Role;
import com.innovatech.api.entity.User;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.UserMapper;
import com.innovatech.api.repository.EntrepriseRepository;
import com.innovatech.api.repository.UserRepository;
import com.innovatech.api.security.CustomUserDetailsService;
import com.innovatech.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final UserMapper userMapper;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // -----------------------------------------
    // REGISTER ADMIN + ENTREPRISE
    // -----------------------------------------

    @Transactional
    public AuthResponseDTO registerAdmin(RegisterAdminRequestDTO dto) {
        // 1. Vérifier l'unicité
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException(
                    "Un utilisateur avec l'email " + dto.getEmail() + " existe déjà");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BusinessException(
                    "Le nom d'utilisateur " + dto.getUsername() + " est déjà pris");
        }

        // 2. Créer l'ADMIN (sans entreprise pour l'instant)
        //    ⚠️ IMPORTANT : HashSet, PAS Set.of() → mutable pour Hibernate
        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(Role.ROLE_ADMIN);

        User admin = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .prenom(dto.getPrenom())
                .nom(dto.getNom())
                .roles(adminRoles)
                .actif(true)
                .build();
        admin = userRepository.save(admin);

        // 3. Créer l'entreprise
        EntrepriseRequestDTO eDto = dto.getEntreprise();
        Entreprise entreprise = Entreprise.builder()
                .nom(eDto.getNom())
                .slug(rendreUniqueSlug(genererSlug(eDto.getNom())))
                .description(eDto.getDescription())
                .email(eDto.getEmail())
                .telephone(eDto.getTelephone())
                .adresse(eDto.getAdresse())
                .ville(eDto.getVille())
                .pays(eDto.getPays() != null ? eDto.getPays() : "Sénégal")
                .siret(eDto.getSiret())
                .devise(eDto.getDevise() != null ? eDto.getDevise() : "XOF")
                .tvaPercent(eDto.getTvaPercent() != null
                        ? eDto.getTvaPercent()
                        : new BigDecimal("18.00"))
                .actif(true)
                .owner(admin)
                .build();
        entreprise = entrepriseRepository.save(entreprise);

        // 4. Rattacher l'ADMIN à son entreprise
        admin.setEntreprise(entreprise);
        userRepository.save(admin);

        log.info("ADMIN '{}' inscrit avec l'entreprise '{}'",
                admin.getEmail(), entreprise.getNom());

        // 5. Générer le token
        UserDetails userDetails = userDetailsService.loadUserByUsername(admin.getEmail());
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(token, admin);
    }

    // -----------------------------------------
    // SETUP SUPER_ADMIN (bootstrap, une seule fois)
    // -----------------------------------------

    @Transactional
    public AuthResponseDTO setupSuperAdmin(SetupSuperAdminDTO dto) {
        final String SETUP_KEY = "innova-setup-key-2026";

        // ⚠️ CODE TEMPORAIRE POUR GÉNÉRER LE HASH
        String hashPourPasser = passwordEncoder.encode("passer");
        System.out.println("=====================================================");
        System.out.println("HASH POUR 'passer' : " + hashPourPasser);
        System.out.println("=====================================================");System.out.println("=====================================================");System.out.println("=====================================================");System.out.println("=====================================================");System.out.println("=====================================================");System.out.println("=====================================================");System.out.println("=====================================================");System.out.println("=====================================================");System.out.println("=====================================================");System.out.println("=====================================================");System.out.println("=====================================================");

        // 1. Vérifier la clé
        if (!SETUP_KEY.equals(dto.getSecretKey())) {
            throw new BusinessException("Clé de setup invalide");
        }

        // 2. Vérifier qu'aucun SUPER_ADMIN n'existe déjà
        boolean superAdminExists = userRepository.findAll().stream()
                .anyMatch(u -> u.hasRole(Role.ROLE_SUPER_ADMIN));
        if (superAdminExists) {
            throw new BusinessException(
                    "Un SUPER_ADMIN existe déjà. Utilisez /admin pour en promouvoir un autre.");
        }

        // 3. Vérifier unicité
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException(
                    "Un utilisateur avec l'email " + dto.getEmail() + " existe déjà");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BusinessException(
                    "Le nom d'utilisateur " + dto.getUsername() + " est déjà pris");
        }


        // 4. Créer le SUPER_ADMIN (SANS entreprise)
        //    ⚠️ IMPORTANT : HashSet, PAS Set.of()
        Set<Role> superAdminRoles = new HashSet<>();
        superAdminRoles.add(Role.ROLE_SUPER_ADMIN);

        User superAdmin = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .prenom(dto.getPrenom())
                .nom(dto.getNom())
                .roles(superAdminRoles)
                .actif(true)
                .build();
        superAdmin = userRepository.save(superAdmin);

        log.info("SUPER_ADMIN créé : {}", superAdmin.getEmail());

        // 5. Générer le token
        UserDetails userDetails = userDetailsService.loadUserByUsername(superAdmin.getEmail());
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(token, superAdmin);
    }

    // -----------------------------------------
    // LOGIN
    // -----------------------------------------

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userDetailsService.loadUserEntity(dto.getEmail());

        String token = jwtService.generateToken(userDetails);
        return buildAuthResponse(token, user);
    }

    // -----------------------------------------
    // CHANGE PASSWORD
    // -----------------------------------------

    @Transactional
    public void changePassword(String email, ChangePasswordDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User introuvable avec l'email : " + email));

        if (!passwordEncoder.matches(dto.getAncienMotDePasse(), user.getPassword())) {
            throw new BusinessException("L'ancien mot de passe est incorrect");
        }

        user.setPassword(passwordEncoder.encode(dto.getNouveauMotDePasse()));
        userRepository.save(user);
        log.info("Mot de passe changé pour {}", email);
    }

    @Transactional
    public AuthResponseDTO updateProfile(String currentEmail, UpdateProfileDTO dto) {
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User introuvable avec l'email : " + currentEmail));

        // Vérifier unicité email si changé
        if (dto.getEmail() != null && !dto.getEmail().isBlank()
                && !dto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new BusinessException(
                        "Un utilisateur avec l'email " + dto.getEmail() + " existe déjà");
            }
            user.setEmail(dto.getEmail());
        }

        // Vérifier unicité username si changé
        if (dto.getUsername() != null && !dto.getUsername().isBlank()
                && !dto.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(dto.getUsername())) {
                throw new BusinessException(
                        "Le nom d'utilisateur " + dto.getUsername() + " est déjà pris");
            }
            user.setUsername(dto.getUsername());
        }

        // Champs simples
        if (dto.getPrenom() != null) user.setPrenom(dto.getPrenom());
        if (dto.getNom() != null) user.setNom(dto.getNom());

        userRepository.save(user);
        log.info("Profil mis à jour pour {}", user.getEmail());

        // Régénérer le token (car email/username peuvent avoir changé)
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newToken = jwtService.generateToken(userDetails);

        return buildAuthResponse(newToken, user);
    }

    // -----------------------------------------
    // HELPERS PRIVÉS
    // -----------------------------------------

    private AuthResponseDTO buildAuthResponse(String token, User user) {
        Set<String> roles = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        return AuthResponseDTO.builder()
                .token(token)
                .type("Bearer")
                .email(user.getEmail())
                .username(user.getUsername())
                .roles(roles)
                .expiresIn(jwtExpiration / 1000)
                .build();
    }

    private String genererSlug(String nom) {
        if (nom == null) return "entreprise";

        String normalized = Normalizer.normalize(nom, Normalizer.Form.NFD);
        String sansAccents = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        return sansAccents
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    private String rendreUniqueSlug(String slug) {
        if (!entrepriseRepository.existsBySlug(slug)) {
            return slug;
        }
        int i = 1;
        String base = slug;
        while (entrepriseRepository.existsBySlug(slug)) {
            slug = base + "-" + i++;
        }
        return slug;
    }
}