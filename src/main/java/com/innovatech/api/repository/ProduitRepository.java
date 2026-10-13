package com.innovatech.api.repository;

import com.innovatech.api.entity.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

    // -----------------------------------------
    // Scoped par entreprise
    // -----------------------------------------

    List<Produit> findByEntrepriseId(Long entrepriseId);

    List<Produit> findByEntrepriseIdAndActifTrue(Long entrepriseId);

    List<Produit> findByEntrepriseIdAndCategorieId(Long entrepriseId, Long categorieId);

    Optional<Produit> findByReferenceAndEntrepriseId(String reference, Long entrepriseId);

    boolean existsByReferenceAndEntrepriseId(String reference, Long entrepriseId);

    long countByEntrepriseId(Long entrepriseId);

    long countByEntrepriseIdAndActifTrue(Long entrepriseId);

    // -----------------------------------------
    // Scoped par entreprise + user (boutique)
    // -----------------------------------------

    List<Produit> findByEntrepriseIdAndUserId(Long entrepriseId, Long userId);

    List<Produit> findByEntrepriseIdAndUserIdAndActifTrue(Long entrepriseId, Long userId);

    Optional<Produit> findByReferenceAndEntrepriseIdAndUserId(
            String reference, Long entrepriseId, Long userId);

    boolean existsByReferenceAndEntrepriseIdAndUserId(
            String reference, Long entrepriseId, Long userId);

    long countByEntrepriseIdAndUserId(Long entrepriseId, Long userId);

    long countByEntrepriseIdAndUserIdAndActifTrue(Long entrepriseId, Long userId);

    // -----------------------------------------
    // Recherche — niveau ENTREPRISE (ADMIN)
    // -----------------------------------------

    @Query("SELECT p FROM Produit p " +
            "WHERE p.entreprise.id = :entrepriseId " +
            "AND LOWER(p.nom) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Produit> searchByEntrepriseId(
            @Param("entrepriseId") Long entrepriseId,
            @Param("query") String query);

    @Query("SELECT p FROM Produit p " +
            "WHERE p.entreprise.id = :entrepriseId " +
            "AND p.stock < :seuil")
    List<Produit> findByEntrepriseIdAndStockLessThan(
            @Param("entrepriseId") Long entrepriseId,
            @Param("seuil") Integer seuil);

    // -----------------------------------------
    // Recherche — niveau BOUTIQUE (USER/SELLER)
    // -----------------------------------------

    @Query("SELECT p FROM Produit p " +
            "WHERE p.entreprise.id = :entrepriseId " +
            "AND p.user.id = :userId " +
            "AND LOWER(p.nom) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Produit> searchByEntrepriseIdAndUserId(
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId,
            @Param("query") String query);

    @Query("SELECT p FROM Produit p " +
            "WHERE p.entreprise.id = :entrepriseId " +
            "AND p.user.id = :userId " +
            "AND p.stock < :seuil")
    List<Produit> findByEntrepriseIdAndUserIdAndStockLessThan(
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId,
            @Param("seuil") Integer seuil);

    // -----------------------------------------
    // Avec JOIN FETCH
    // -----------------------------------------

    @Query("SELECT DISTINCT p FROM Produit p " +
            "LEFT JOIN FETCH p.categorie " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN FETCH p.entreprise " +
            "LEFT JOIN FETCH p.photos " +
            "WHERE p.id = :id")
    Optional<Produit> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT p FROM Produit p " +
            "LEFT JOIN FETCH p.categorie " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN FETCH p.entreprise " +
            "LEFT JOIN FETCH p.photos " +
            "WHERE p.id = :id AND p.entreprise.id = :entrepriseId")
    Optional<Produit> findByIdAndEntrepriseIdWithDetails(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId);

    @Query("SELECT DISTINCT p FROM Produit p " +
            "LEFT JOIN FETCH p.categorie " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN FETCH p.entreprise " +
            "LEFT JOIN FETCH p.photos " +
            "WHERE p.id = :id AND p.user.id = :userId")
    Optional<Produit> findByIdAndUserIdWithDetails(
            @Param("id") Long id,
            @Param("userId") Long userId);

    @Query("SELECT DISTINCT p FROM Produit p " +
            "LEFT JOIN FETCH p.categorie " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN FETCH p.entreprise " +
            "LEFT JOIN FETCH p.photos " +
            "WHERE p.id = :id " +
            "AND p.entreprise.id = :entrepriseId " +
            "AND p.user.id = :userId")
    Optional<Produit> findByIdAndEntrepriseIdAndUserIdWithDetails(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    @Query("SELECT DISTINCT p FROM Produit p " +
            "LEFT JOIN FETCH p.categorie " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN FETCH p.entreprise " +
            "WHERE p.entreprise.id = :entrepriseId " +
            "ORDER BY p.id")
    List<Produit> findByEntrepriseIdWithDetails(@Param("entrepriseId") Long entrepriseId);

    @Query("SELECT DISTINCT p FROM Produit p " +
            "LEFT JOIN FETCH p.categorie " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN FETCH p.entreprise " +
            "WHERE p.entreprise.id = :entrepriseId AND p.actif = true")
    List<Produit> findByEntrepriseIdAndActifTrueWithDetails(
            @Param("entrepriseId") Long entrepriseId);

    @Query("SELECT DISTINCT p FROM Produit p " +
            "LEFT JOIN FETCH p.categorie " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN FETCH p.entreprise " +
            "WHERE p.entreprise.id = :entrepriseId " +
            "AND p.user.id = :userId " +
            "ORDER BY p.id")
    List<Produit> findByEntrepriseIdAndUserIdWithDetails(
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    @Query("SELECT DISTINCT p FROM Produit p " +
            "LEFT JOIN FETCH p.categorie " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN FETCH p.entreprise " +
            "WHERE p.entreprise.id = :entrepriseId " +
            "AND p.user.id = :userId " +
            "AND p.actif = true " +
            "ORDER BY p.id")
    List<Produit> findByEntrepriseIdAndUserIdAndActifTrueWithDetails(
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    // -----------------------------------------
    // Recherche par catégorie (récursive - MySQL 8+)
    // -----------------------------------------

    @Query(value = """
        WITH RECURSIVE tree AS (
            SELECT id FROM categories WHERE id = :categorieId AND entreprise_id = :entrepriseId
            UNION ALL
            SELECT c.id FROM categories c
            JOIN tree t ON c.parent_id = t.id
            WHERE c.entreprise_id = :entrepriseId
        )
        SELECT p.* FROM produits p
        WHERE p.categorie_id IN (SELECT id FROM tree)
          AND p.entreprise_id = :entrepriseId
        """, nativeQuery = true)
    List<Produit> findByCategorieAndDescendants(
            @Param("categorieId") Long categorieId,
            @Param("entrepriseId") Long entrepriseId
    );
}