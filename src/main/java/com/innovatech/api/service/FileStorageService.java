package com.innovatech.api.service;

import com.innovatech.api.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service de stockage physique des fichiers.
 */
@Service
@Slf4j
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.allowed-types}")
    private String allowedTypes;

    /**
     * Sauvegarde un fichier et retourne son nom unique.
     */
    public String store(MultipartFile file, String sousDossier) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Le fichier est vide");
        }

        // Vérifier le type
        List<String> allowed = Arrays.asList(allowedTypes.split(","));
        if (!allowed.contains(file.getContentType())) {
            throw new BusinessException(
                    "Type de fichier non autorisé : " + file.getContentType()
                            + ". Types acceptés : " + allowedTypes);
        }

        // Générer un nom unique
        String extension = getExtension(file.getOriginalFilename());
        String nomFichier = UUID.randomUUID().toString() + extension;

        try {
            Path dossier = Paths.get(uploadDir, sousDossier);
            Files.createDirectories(dossier);

            Path chemin = dossier.resolve(nomFichier);
            Files.copy(file.getInputStream(), chemin, StandardCopyOption.REPLACE_EXISTING);

            log.info("Fichier stocké : {}", chemin.toAbsolutePath());
            return nomFichier;
        } catch (IOException e) {
            throw new RuntimeException("Erreur de stockage du fichier", e);
        }
    }

    /**
     * Supprime un fichier.
     */
    public void delete(String nomFichier, String sousDossier) {
        try {
            Path chemin = Paths.get(uploadDir, sousDossier, nomFichier);
            Files.deleteIfExists(chemin);
            log.info("Fichier supprimé : {}", chemin.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Impossible de supprimer le fichier : {}", nomFichier);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}