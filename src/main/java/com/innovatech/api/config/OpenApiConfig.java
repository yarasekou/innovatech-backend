package com.innovatech.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration OpenAPI / Swagger UI pour InnovaTech API.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("InnovaTech API")
                        .description("""
                    API REST de gestion pour InnovaTech.
                    
                    **Modules disponibles :**
                    - 👤 Authentification (register, login)
                    - 👥 Clients (CRUD + recherche)
                    - 📦 Produits (CRUD + stock)
                    - 📋 Commandes (CRUD + statuts + lignes)
                    - 🔐 Admin (users, promotion)
                    
                    **Comment s'authentifier :**
                    1. Appelez `POST /auth/login` avec vos identifiants
                    2. Copiez le `token` de la réponse
                    3. Cliquez sur le bouton **Authorize** en haut à droite
                    4. Collez le token (sans "Bearer ")
                    5. Tous les appels suivants seront authentifiés
                    """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("InnovaTech")
                                .email("contact@innovatech.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/api")
                                .description("Serveur de développement")
                ))
                .addSecurityItem(new SecurityRequirement()
                        .addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Entrez votre token JWT (sans 'Bearer ')")));
    }
}