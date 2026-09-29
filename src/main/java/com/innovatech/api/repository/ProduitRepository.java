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

    Optional<Produit> findByReference(String reference);

    boolean existsByReference(String reference);

    List<Produit> findByActifTrue();

    List<Produit> findByNomContainingIgnoreCase(String nom);

    List<Produit> findByStockLessThan(Integer seuil);

    /**
     * Produits par catégorie (directement attachés).
     */
    List<Produit> findByCategorieId(Long categorieId);

    /**
     * Charge un produit avec sa catégorie (évite N+1).
     */
    @Query("SELECT p FROM Produit p LEFT JOIN FETCH p.categorie WHERE p.id = :id")
    Optional<Produit> findByIdWithCategorie(@Param("id") Long id);

    @Query("SELECT p FROM Produit p LEFT JOIN FETCH p.categorie ORDER BY p.id")
    List<Produit> findAllWithCategorie();

    @Query("SELECT p FROM Produit p LEFT JOIN FETCH p.categorie WHERE p.actif = true")
    List<Produit> findByActifTrueWithCategorie();

    /**
     * Produits d'une catégorie ET de ses descendants (requête récursive MySQL 8+).
     */
    @Query(value = """
        WITH RECURSIVE tree AS (
            SELECT id FROM categories WHERE id = :categorieId
            UNION ALL
            SELECT c.id FROM categories c
            JOIN tree t ON c.parent_id = t.id
        )
        SELECT p.* FROM produits p
        WHERE p.categorie_id IN (SELECT id FROM tree)
        """, nativeQuery = true)
    List<Produit> findByCategorieAndDescendants(@Param("categorieId") Long categorieId);

    @Query("SELECT p FROM Produit p LEFT JOIN FETCH p.categorie LEFT JOIN FETCH p.user WHERE p.id = :id")
    Optional<Produit> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT p FROM Produit p LEFT JOIN FETCH p.categorie LEFT JOIN FETCH p.user ORDER BY p.id")
    List<Produit> findAllWithDetails();

    @Query("SELECT p FROM Produit p LEFT JOIN FETCH p.categorie LEFT JOIN FETCH p.user WHERE p.actif = true")
    List<Produit> findByActifTrueWithDetails();

    @Query("SELECT p FROM Produit p LEFT JOIN FETCH p.categorie LEFT JOIN FETCH p.user WHERE p.user.id = :userId")
    List<Produit> findByUserIdWithDetails(@Param("userId") Long userId);

    @Query("SELECT p FROM Produit p LEFT JOIN FETCH p.categorie LEFT JOIN FETCH p.user " +
            "WHERE p.user.id = :userId AND p.actif = true")
    List<Produit> findByUserIdAndActifTrueWithDetails(@Param("userId") Long userId);

    @Query("SELECT p FROM Produit p LEFT JOIN FETCH p.categorie LEFT JOIN FETCH p.user " +
            "WHERE p.id = :id AND p.user.id = :userId")
    Optional<Produit> findByIdAndUserIdWithDetails(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT p FROM Produit p LEFT JOIN FETCH p.categorie LEFT JOIN FETCH p.user " +
            "WHERE p.user.id = :userId AND LOWER(p.nom) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Produit> searchByUserId(@Param("userId") Long userId, @Param("query") String query);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM Produit p WHERE p.reference = :reference AND p.user.id = :userId")
    boolean existsByReferenceAndUserId(@Param("reference") String reference,
                                       @Param("userId") Long userId);
}