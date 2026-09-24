package com.innovatech.api.repository;

import com.innovatech.api.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour l'entité {@link Client}.
 * <p>
 * Spring Data JPA génère automatiquement l'implémentation à partir
 * des signatures de méthodes.
 */
@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    /**
     * Recherche un client par son email (unique).
     */
    Optional<Client> findByEmail(String email);

    /**
     * Vérifie si un email est déjà utilisé.
     */
    boolean existsByEmail(String email);

    /**
     * Liste tous les clients actifs.
     */
    List<Client> findByActifTrue();

    /**
     * Recherche par ville (insensible à la casse).
     */
    List<Client> findByVilleIgnoreCase(String ville);

    /**
     * Recherche par nom ou prénom contenant une chaîne.
     */
    List<Client> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(
            String nom, String prenom
    );
}