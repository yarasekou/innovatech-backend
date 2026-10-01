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
            Path uploadPath = Paths.get(uploadDir, "produits");
            Files.createDirectories(uploadPath);
            log.info("Dossier d'upload prêt : {}", uploadPath.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le dossier d'upload", e);
        }
    }
}