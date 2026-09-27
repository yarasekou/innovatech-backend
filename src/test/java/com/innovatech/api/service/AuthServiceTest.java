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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Tests unitaires")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private User user;
    private RegisterRequestDTO registerDTO;
    private LoginRequestDTO loginDTO;

    @BeforeEach
    void setUp() {
        // Injecter la valeur @Value manuellement
        ReflectionTestUtils.setField(authService, "jwtExpiration", 86400000L);

        user = User.builder()
                .id(1L)
                .username("yara")
                .email("yara@innovatech.com")
                .password("$2a$12$hashedPassword")
                .prenom("Yara")
                .nom("Sékou")
                .roles(Set.of(Role.ROLE_USER))
                .actif(true)
                .build();

        registerDTO = RegisterRequestDTO.builder()
                .username("yara")
                .email("yara@innovatech.com")
                .password("password123")
                .build();

        loginDTO = LoginRequestDTO.builder()
                .email("yara@innovatech.com")
                .password("password123")
                .build();
    }

    // =========================================
    // REGISTER
    // =========================================

    @Test
    @DisplayName("Register - Devrait créer un utilisateur et retourner un JWT")
    void shouldRegisterUserAndReturnToken() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("yara@innovatech.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("fake-jwt-token");

        AuthResponseDTO result = authService.register(registerDTO);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("fake-jwt-token");
        assertThat(result.getEmail()).isEqualTo("yara@innovatech.com");
        assertThat(result.getRoles()).contains("ROLE_USER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Register - Devrait lever BusinessException si email existe")
    void shouldThrowWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("yara@innovatech.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("email");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Register - Devrait lever BusinessException si username existe")
    void shouldThrowWhenUsernameAlreadyExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("yara")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("utilisateur");
    }

    // =========================================
    // LOGIN
    // =========================================

    @Test
    @DisplayName("Login - Devrait retourner un token si identifiants valides")
    void shouldLoginAndReturnToken() {
        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetailsService.loadUserEntity("yara@innovatech.com")).thenReturn(user);

        when(jwtService.generateToken(userDetails)).thenReturn("fake-jwt-token");   // ⚠️ utilise le MÊME userDetails

        AuthResponseDTO result = authService.login(loginDTO);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("fake-jwt-token");
        assertThat(result.getUsername()).isEqualTo("yara");
    }

    @Test
    @DisplayName("Login - Devrait propager BadCredentialsException si mauvais mdp")
    void shouldThrowWhenBadCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginDTO))
                .isInstanceOf(BadCredentialsException.class);
    }
}