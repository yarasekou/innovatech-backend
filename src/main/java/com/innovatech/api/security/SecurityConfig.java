package com.innovatech.api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration Spring Security pour l'API.
 * <p>
 * Hiérarchie des rôles :
 * <ul>
 *   <li>ROLE_SUPER_ADMIN : plateforme InnovaTech — voit TOUT</li>
 *   <li>ROLE_ADMIN       : owner d'UNE entreprise — voit TOUT dans SON entreprise</li>
 *   <li>ROLE_USER        : manager interne (boutique) — voit SES données</li>
 *   <li>ROLE_SELLER      : vendeur terrain — voit les données de son patron (USER)</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Désactiver CSRF (API REST stateless)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Désactiver CORS par défaut
                .cors(AbstractHttpConfigurer::disable)

                // 3. Stateless (pas de session HTTP)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Autorisation des requêtes
                .authorizeHttpRequests(auth -> auth
                        // -----------------------------------------
                        // Routes publiques
                        // -----------------------------------------
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // -----------------------------------------
                        // Endpoints plateforme (SUPER_ADMIN uniquement)
                        // -----------------------------------------
                        .requestMatchers("/admin/**").hasRole("SUPER_ADMIN")

                        // -----------------------------------------
                        // Endpoints USERs (créés par un ADMIN)
                        // ⚠️ Un ADMIN crée ses USERs
                        // -----------------------------------------
                        .requestMatchers("/users/**").hasRole("ADMIN")

                        // -----------------------------------------
                        // Endpoints SELLERs (créés par un USER)
                        // -----------------------------------------
                        .requestMatchers("/sellers/**").hasRole("USER")

                        // -----------------------------------------
                        // Endpoints entreprise
                        // Accessible à tous les rôles "tenant"
                        // -----------------------------------------
                        .requestMatchers("/entreprises/**")
                        .hasAnyRole("ADMIN", "USER", "SELLER")

                        // -----------------------------------------
                        // Tout le reste : authentifié
                        // Le filtrage fin (par rôle / entreprise / user)
                        // est fait dans les services
                        // -----------------------------------------
                        .anyRequest().authenticated()
                )

                // 5. Provider d'authentification
                .authenticationProvider(authenticationProvider())

                // 6. Filtre JWT
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}