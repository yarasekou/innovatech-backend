package com.innovatech.api.repository;

import com.innovatech.api.entity.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

    Optional<Produit> findByReference(String reference);

    boolean existsByReference(String reference);

    List<Produit> findByActifTrue();

    List<Produit> findByCategorieIgnoreCase(String categorie);

    List<Produit> findByNomContainingIgnoreCase(String nom);

    /**
     * Produits dont le stock est sous le seuil donné.
     */
    List<Produit> findByStockLessThan(Integer seuil);
}