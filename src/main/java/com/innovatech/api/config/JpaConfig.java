package com.innovatech.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration JPA.
 * <p>
 * Active l'audit automatique des entités (@CreatedDate, @LastModifiedDate).
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {

}