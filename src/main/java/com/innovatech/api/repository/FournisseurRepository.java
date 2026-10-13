package com.innovatech.api.repository;

import com.innovatech.api.entity.Fournisseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {

    // -----------------------------------------
    // Scoped par entreprise
    // -----------------------------------------

    List<Fournisseur> findByEntrepriseId(Long entrepriseId);

    List<Fournisseur> findByEntrepriseIdAndActifTrue(Long entrepriseId);

    long countByEntrepriseId(Long entrepriseId);

    long countByEntrepriseIdAndActifTrue(Long entrepriseId);

    // -----------------------------------------
    // Scoped par entreprise + user (boutique)
    // -----------------------------------------

    Optional<Fournisseur> findByCodeAndEntrepriseIdAndUserId(
            String code, Long entrepriseId, Long userId);

    Optional<Fournisseur> findByEmailAndEntrepriseIdAndUserId(
            String email, Long entrepriseId, Long userId);

    boolean existsByCodeAndEntrepriseIdAndUserId(
            String code, Long entrepriseId, Long userId);

    boolean existsByEmailAndEntrepriseIdAndUserId(
            String email, Long entrepriseId, Long userId);

    List<Fournisseur> findByEntrepriseIdAndUserId(Long entrepriseId, Long userId);

    List<Fournisseur> findByEntrepriseIdAndUserIdAndActifTrue(Long entrepriseId, Long userId);

    long countByEntrepriseIdAndUserId(Long entrepriseId, Long userId);

    // -----------------------------------------
    // Recherche
    // -----------------------------------------

    @Query("SELECT f FROM Fournisseur f " +
            "WHERE f.entreprise.id = :entrepriseId " +
            "AND LOWER(f.nom) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Fournisseur> searchByEntrepriseId(@Param("entrepriseId") Long entrepriseId,
                                           @Param("query") String query);

    @Query("SELECT f FROM Fournisseur f " +
            "WHERE f.entreprise.id = :entrepriseId " +
            "AND f.user.id = :userId " +
            "AND LOWER(f.nom) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Fournisseur> searchByEntrepriseIdAndUserId(
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId,
            @Param("query") String query);

    @Query("SELECT f FROM Fournisseur f " +
            "WHERE f.entreprise.id = :entrepriseId " +
            "AND LOWER(f.pays) = LOWER(:pays)")
    List<Fournisseur> findByEntrepriseIdAndPays(@Param("entrepriseId") Long entrepriseId,
                                                @Param("pays") String pays);

    // -----------------------------------------
    // Avec JOIN FETCH
    // -----------------------------------------

    @Query("SELECT f FROM Fournisseur f " +
            "LEFT JOIN FETCH f.user " +
            "LEFT JOIN FETCH f.entreprise " +
            "WHERE f.id = :id")
    Optional<Fournisseur> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT f FROM Fournisseur f " +
            "LEFT JOIN FETCH f.user " +
            "LEFT JOIN FETCH f.entreprise " +
            "WHERE f.id = :id AND f.entreprise.id = :entrepriseId")
    Optional<Fournisseur> findByIdAndEntrepriseIdWithDetails(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId);

    @Query("SELECT f FROM Fournisseur f " +
            "LEFT JOIN FETCH f.user " +
            "LEFT JOIN FETCH f.entreprise " +
            "WHERE f.id = :id " +
            "AND f.entreprise.id = :entrepriseId " +
            "AND f.user.id = :userId")
    Optional<Fournisseur> findByIdAndEntrepriseIdAndUserIdWithDetails(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    @Query("SELECT f FROM Fournisseur f " +
            "LEFT JOIN FETCH f.user " +
            "LEFT JOIN FETCH f.entreprise " +
            "WHERE f.entreprise.id = :entrepriseId " +
            "ORDER BY f.id")
    List<Fournisseur> findByEntrepriseIdWithDetails(@Param("entrepriseId") Long entrepriseId);

    @Query("SELECT f FROM Fournisseur f " +
            "LEFT JOIN FETCH f.user " +
            "LEFT JOIN FETCH f.entreprise " +
            "WHERE f.entreprise.id = :entrepriseId AND f.actif = true " +
            "ORDER BY f.id")
    List<Fournisseur> findByEntrepriseIdAndActifTrueWithDetails(
            @Param("entrepriseId") Long entrepriseId);

    @Query("SELECT f FROM Fournisseur f " +
            "LEFT JOIN FETCH f.user " +
            "LEFT JOIN FETCH f.entreprise " +
            "WHERE f.entreprise.id = :entrepriseId " +
            "AND f.user.id = :userId " +
            "ORDER BY f.id")
    List<Fournisseur> findByEntrepriseIdAndUserIdWithDetails(
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    @Query("SELECT f FROM Fournisseur f " +
            "LEFT JOIN FETCH f.user " +
            "LEFT JOIN FETCH f.entreprise " +
            "WHERE f.entreprise.id = :entrepriseId " +
            "AND f.user.id = :userId " +
            "AND f.actif = true " +
            "ORDER BY f.id")
    List<Fournisseur> findByEntrepriseIdAndUserIdAndActifTrueWithDetails(
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    @Query("SELECT f FROM Fournisseur f " +
            "LEFT JOIN FETCH f.user " +
            "LEFT JOIN FETCH f.entreprise " +
            "WHERE f.code = :code " +
            "AND f.entreprise.id = :entrepriseId " +
            "AND f.user.id = :userId")
    Optional<Fournisseur> findByCodeAndEntrepriseIdAndUserIdWithDetails(
            @Param("code") String code,
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);
}