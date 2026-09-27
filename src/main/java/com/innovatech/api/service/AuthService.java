package com.innovatech.api.service;

import com.innovatech.api.dto.AuthResponseDTO;
import com.innovatech.api.dto.LoginRequestDTO;
import com.innovatech.api.dto.RegisterRequestDTO;
import com.innovatech.api.entity.Role;
import com.innovatech.api.entity.User;
import com.innovatech.api.exception.BusinessException;
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

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // -----------------------------------------
    // REGISTER
    // -----------------------------------------

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("Un utilisateur avec l'email " + dto.getEmail() + " existe déjà");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BusinessException("Le nom d'utilisateur " + dto.getUsername() + " est déjà pris");
        }

        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .prenom(dto.getPrenom())
                .nom(dto.getNom())
                .roles(Set.of(Role.ROLE_USER))   // par défaut
                .actif(true)
                .build();

        userRepository.save(user);
        log.info("Nouvel utilisateur créé : {}", user.getEmail());

        // Générer le token immédiatement
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(token, user);
    }

    // -----------------------------------------
    // LOGIN
    // -----------------------------------------

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO dto) {
        // Authentifie (lance BadCredentialsException si mauvais)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userDetailsService.loadUserEntity(dto.getEmail());

        assert userDetails != null;
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(token, user);
    }

    // -----------------------------------------
    // Helper
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
                .expiresIn(jwtExpiration / 1000)   // en secondes
                .build();
    }
}