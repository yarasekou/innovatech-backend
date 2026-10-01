package com.innovatech.api.repository;

import com.innovatech.api.entity.PhotoProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhotoProduitRepository extends JpaRepository<PhotoProduit, Long> {

    /**
     * Liste les photos d'un produit, triées par ordre.
     */
    List<PhotoProduit> findByProduitIdOrderByOrdreAsc(Long produitId);

    /**
     * Récupère la photo principale d'un produit.
     */
    Optional<PhotoProduit> findByProduitIdAndPrincipaleTrue(Long produitId);

    /**
     * Compte le nombre de photos d'un produit.
     */
    long countByProduitId(Long produitId);

    /**
     * Liste les photos d'un produit avec le produit chargé (évite N+1).
     */
    @Query("SELECT p FROM PhotoProduit p LEFT JOIN FETCH p.produit WHERE p.produit.id = :produitId ORDER BY p.ordre ASC")
    List<PhotoProduit> findByProduitIdWithProduit(@Param("produitId") Long produitId);

    /**
     * Récupère une photo avec son produit.
     */
    @Query("SELECT p FROM PhotoProduit p LEFT JOIN FETCH p.produit WHERE p.id = :id")
    Optional<PhotoProduit> findByIdWithProduit(@Param("id") Long id);

    /**
     * Supprime toutes les photos d'un produit.
     */
    void deleteByProduitId(Long produitId);
}