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

    Optional<Categorie> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Categorie> findByParentIsNull();

    List<Categorie> findByParentId(Long parentId);

    List<Categorie> findByActifTrue();

    List<Categorie> findByNiveau(Integer niveau);

    long countByParentId(Long parentId);

    /**
     * Charge une catégorie avec son parent (pour éviter le N+1).
     */
    @Query("SELECT c FROM Categorie c LEFT JOIN FETCH c.parent WHERE c.id = :id")
    Optional<Categorie> findByIdWithParent(@Param("id") Long id);

    /**
     * Charge toutes les catégories racines avec leurs enfants directs.
     */
    @Query("SELECT DISTINCT c FROM Categorie c LEFT JOIN FETCH c.enfants WHERE c.parent IS NULL ORDER BY c.nom")
    List<Categorie> findRacinesWithEnfants();

    /**
     * Charge une catégorie avec ses enfants directs.
     */
    @Query("SELECT DISTINCT c FROM Categorie c LEFT JOIN FETCH c.enfants WHERE c.id = :id")
    Optional<Categorie> findByIdWithEnfants(@Param("id") Long id);

    /**
     * Catégories feuilles (celles qui n'ont AUCUN enfant).
     */
    @Query("""
    SELECT c FROM Categorie c
    WHERE c.actif = true
      AND NOT EXISTS (SELECT 1 FROM Categorie enfant WHERE enfant.parent.id = c.id)
    ORDER BY c.cheminComplet
    """)
    List<Categorie> findFeuilles();

    /**
     * Catégories feuilles descendantes d'une catégorie donnée
     * (récursif via chemin_complet).
     */
    @Query("""
    SELECT c FROM Categorie c
    WHERE c.actif = true
      AND c.cheminComplet LIKE CONCAT(:chemin, '%')
      AND c.id <> :categorieId
      AND NOT EXISTS (SELECT 1 FROM Categorie enfant WHERE enfant.parent.id = c.id)
    ORDER BY c.cheminComplet
    """)
    List<Categorie> findFeuillesDescendants(
            @Param("categorieId") Long categorieId,
            @Param("chemin") String chemin
    );
}