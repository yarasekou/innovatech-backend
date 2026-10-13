package com.innovatech.api.repository;

import com.innovatech.api.entity.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategorieRepository extends JpaRepository<Categorie, Long> {

    // -----------------------------------------
    // Scoped par entreprise (ADMIN)
    // -----------------------------------------

    List<Categorie> findByEntrepriseId(Long entrepriseId);

    List<Categorie> findByEntrepriseIdAndActifTrue(Long entrepriseId);

    List<Categorie> findByEntrepriseIdAndParentIsNull(Long entrepriseId);

    // -----------------------------------------
    // Scoped par entreprise + user (boutique)
    // -----------------------------------------

    Optional<Categorie> findBySlugAndEntrepriseIdAndUserId(
            String slug, Long entrepriseId, Long userId);

    boolean existsBySlugAndEntrepriseIdAndUserId(
            String slug, Long entrepriseId, Long userId);

    List<Categorie> findByEntrepriseIdAndUserId(Long entrepriseId, Long userId);

    List<Categorie> findByEntrepriseIdAndUserIdAndActifTrue(Long entrepriseId, Long userId);

    List<Categorie> findByEntrepriseIdAndUserIdAndParentIsNull(Long entrepriseId, Long userId);

    List<Categorie> findByEntrepriseIdAndUserIdAndParentId(
            Long entrepriseId, Long userId, Long parentId);

    long countByEntrepriseIdAndUserIdAndParentId(
            Long entrepriseId, Long userId, Long parentId);

    // -----------------------------------------
    // Avec JOIN FETCH
    // -----------------------------------------

    @Query("SELECT c FROM Categorie c " +
            "LEFT JOIN FETCH c.parent " +
            "LEFT JOIN FETCH c.entreprise " +
            "LEFT JOIN FETCH c.user " +
            "WHERE c.id = :id")
    Optional<Categorie> findByIdWithParent(@Param("id") Long id);

    @Query("SELECT c FROM Categorie c " +
            "LEFT JOIN FETCH c.parent " +
            "LEFT JOIN FETCH c.user " +
            "WHERE c.id = :id AND c.entreprise.id = :entrepriseId")
    Optional<Categorie> findByIdAndEntrepriseIdWithParent(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId);

    @Query("SELECT c FROM Categorie c " +
            "LEFT JOIN FETCH c.parent " +
            "LEFT JOIN FETCH c.user " +
            "WHERE c.id = :id " +
            "AND c.entreprise.id = :entrepriseId " +
            "AND c.user.id = :userId")
    Optional<Categorie> findByIdAndEntrepriseIdAndUserIdWithParent(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    @Query("SELECT DISTINCT c FROM Categorie c " +
            "LEFT JOIN FETCH c.enfants " +
            "WHERE c.id = :id")
    Optional<Categorie> findByIdWithEnfants(@Param("id") Long id);

    // -----------------------------------------
    // Feuilles (sans enfant) — niveau entreprise
    // -----------------------------------------

    @Query("""
        SELECT c FROM Categorie c
        WHERE c.actif = true
          AND c.entreprise.id = :entrepriseId
          AND NOT EXISTS (
              SELECT 1 FROM Categorie enfant WHERE enfant.parent.id = c.id
          )
        ORDER BY c.cheminComplet
    """)
    List<Categorie> findFeuillesByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    @Query("""
        SELECT c FROM Categorie c
        WHERE c.actif = true
          AND c.entreprise.id = :entrepriseId
          AND c.user.id = :userId
          AND NOT EXISTS (
              SELECT 1 FROM Categorie enfant WHERE enfant.parent.id = c.id
          )
        ORDER BY c.cheminComplet
    """)
    List<Categorie> findFeuillesByEntrepriseIdAndUserId(
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    // -----------------------------------------
    // Feuilles descendantes — niveau entreprise
    // -----------------------------------------

    @Query("""
        SELECT c FROM Categorie c
        WHERE c.actif = true
          AND c.entreprise.id = :entrepriseId
          AND c.cheminComplet LIKE CONCAT(:chemin, '%')
          AND c.id <> :categorieId
          AND NOT EXISTS (
              SELECT 1 FROM Categorie enfant WHERE enfant.parent.id = c.id
          )
        ORDER BY c.cheminComplet
    """)
    List<Categorie> findFeuillesDescendantsByEntrepriseId(
            @Param("categorieId") Long categorieId,
            @Param("entrepriseId") Long entrepriseId,
            @Param("chemin") String chemin
    );

    @Query("""
        SELECT c FROM Categorie c
        WHERE c.actif = true
          AND c.entreprise.id = :entrepriseId
          AND c.user.id = :userId
          AND c.cheminComplet LIKE CONCAT(:chemin, '%')
          AND c.id <> :categorieId
          AND NOT EXISTS (
              SELECT 1 FROM Categorie enfant WHERE enfant.parent.id = c.id
          )
        ORDER BY c.cheminComplet
    """)
    List<Categorie> findFeuillesDescendantsByEntrepriseIdAndUserId(
            @Param("categorieId") Long categorieId,
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId,
            @Param("chemin") String chemin
    );
}