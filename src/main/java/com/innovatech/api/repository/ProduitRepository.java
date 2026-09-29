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
}