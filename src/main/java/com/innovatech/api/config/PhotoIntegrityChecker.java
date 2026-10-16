package com.innovatech.api.config;

import com.innovatech.api.entity.PhotoProduit;
import com.innovatech.api.repository.PhotoProduitRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PhotoIntegrityChecker {

    private final PhotoProduitRepository photoRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostConstruct
    public void checkIntegrity() {
        List<PhotoProduit> photos = photoRepository.findAll();
        int supprimees = 0;

        for (PhotoProduit photo : photos) {
            Path chemin = Paths.get(uploadDir, "produits", photo.getNomFichier());
            if (!Files.exists(chemin)) {
                log.warn("🗑️ Photo fantôme détectée : {} (fichier absent)", photo.getNomFichier());
                photoRepository.delete(photo);
                supprimees++;
            }
        }

        if (supprimees > 0) {
            log.info("✅ {} photo(s) fantôme(s) nettoyée(s)", supprimees);
        } else {
            log.info("✅ Intégrité des photos OK ({} photos vérifiées)", photos.size());
        }
    }
}