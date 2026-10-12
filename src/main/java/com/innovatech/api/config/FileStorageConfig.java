package com.innovatech.api.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration du stockage de fichiers.
 * <p>
 * Crée le dossier d'upload au démarrage si inexistant.
 */
@Configuration
@Slf4j
public class FileStorageConfig {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        try {
            Path produitsPath = Paths.get(uploadDir, "produits");
            Path entreprisesPath = Paths.get(uploadDir, "entreprises");
            Files.createDirectories(produitsPath);
            Files.createDirectories(entreprisesPath);
            log.info("Dossiers d'upload prêts : {} et {}",
                    produitsPath.toAbsolutePath(), entreprisesPath.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer les dossiers d'upload", e);
        }
    }
}