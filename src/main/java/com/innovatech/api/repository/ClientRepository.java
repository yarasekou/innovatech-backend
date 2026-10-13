package com.innovatech.api.repository;

import com.innovatech.api.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    // -----------------------------------------
    // Scoped par entreprise
    // -----------------------------------------

    List<Client> findByEntrepriseId(Long entrepriseId);

    List<Client> findByEntrepriseIdAndActifTrue(Long entrepriseId);

    long countByEntrepriseId(Long entrepriseId);

    long countByEntrepriseIdAndActifTrue(Long entrepriseId);

    // -----------------------------------------
    // Scoped par entreprise + user (boutique)
    // -----------------------------------------

    Optional<Client> findByEmailAndEntrepriseIdAndUserId(
            String email, Long entrepriseId, Long userId);

    boolean existsByEmailAndEntrepriseIdAndUserId(
            String email, Long entrepriseId, Long userId);

    List<Client> findByEntrepriseIdAndUserId(Long entrepriseId, Long userId);

    List<Client> findByEntrepriseIdAndUserIdAndActifTrue(Long entrepriseId, Long userId);

    long countByEntrepriseIdAndUserId(Long entrepriseId, Long userId);

    long countByEntrepriseIdAndUserIdAndActifTrue(Long entrepriseId, Long userId);

    // -----------------------------------------
    // Recherche
    // -----------------------------------------

    @Query("SELECT c FROM Client c " +
            "WHERE c.entreprise.id = :entrepriseId " +
            "AND (LOWER(c.nom) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(c.prenom) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Client> searchByEntrepriseId(@Param("entrepriseId") Long entrepriseId,
                                      @Param("query") String query);

    @Query("SELECT c FROM Client c " +
            "WHERE c.entreprise.id = :entrepriseId " +
            "AND c.user.id = :userId " +
            "AND (LOWER(c.nom) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(c.prenom) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Client> searchByEntrepriseIdAndUserId(
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId,
            @Param("query") String query);

    // -----------------------------------------
    // Avec JOIN FETCH
    // -----------------------------------------

    @Query("SELECT c FROM Client c " +
            "LEFT JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.entreprise " +
            "WHERE c.id = :id")
    Optional<Client> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT c FROM Client c " +
            "LEFT JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.entreprise " +
            "WHERE c.id = :id AND c.entreprise.id = :entrepriseId")
    Optional<Client> findByIdAndEntrepriseIdWithDetails(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId);

    @Query("SELECT c FROM Client c " +
            "LEFT JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.entreprise " +
            "WHERE c.id = :id " +
            "AND c.entreprise.id = :entrepriseId " +
            "AND c.user.id = :userId")
    Optional<Client> findByIdAndEntrepriseIdAndUserIdWithDetails(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    @Query("SELECT c FROM Client c " +
            "LEFT JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.entreprise " +
            "WHERE c.entreprise.id = :entrepriseId " +
            "ORDER BY c.id")
    List<Client> findByEntrepriseIdWithDetails(@Param("entrepriseId") Long entrepriseId);

    @Query("SELECT c FROM Client c " +
            "LEFT JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.entreprise " +
            "WHERE c.entreprise.id = :entrepriseId AND c.actif = true " +
            "ORDER BY c.id")
    List<Client> findByEntrepriseIdAndActifTrueWithDetails(
            @Param("entrepriseId") Long entrepriseId);

    @Query("SELECT c FROM Client c " +
            "LEFT JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.entreprise " +
            "WHERE c.entreprise.id = :entrepriseId " +
            "AND c.user.id = :userId " +
            "ORDER BY c.id")
    List<Client> findByEntrepriseIdAndUserIdWithDetails(
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    @Query("SELECT c FROM Client c " +
            "LEFT JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.entreprise " +
            "WHERE c.entreprise.id = :entrepriseId " +
            "AND c.user.id = :userId " +
            "AND c.actif = true " +
            "ORDER BY c.id")
    List<Client> findByEntrepriseIdAndUserIdAndActifTrueWithDetails(
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);
}