package com.innovatech.api.repository;

import com.innovatech.api.entity.StockFournisseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockFournisseurRepository extends JpaRepository<StockFournisseur, Long> {

    Optional<StockFournisseur> findByProduitIdAndFournisseurId(Long produitId, Long fournisseurId);

    List<StockFournisseur> findByProduitId(Long produitId);

    List<StockFournisseur> findByFournisseurId(Long fournisseurId);

    @Query("SELECT COALESCE(SUM(sf.stock), 0) FROM StockFournisseur sf WHERE sf.produit.id = :produitId")
    Integer sumStockByProduitId(@Param("produitId") Long produitId);

    @Query("SELECT sf FROM StockFournisseur sf " +
            "JOIN FETCH sf.produit p " +
            "JOIN FETCH sf.fournisseur f " +
            "WHERE sf.produit.id = :produitId")
    List<StockFournisseur> findByProduitIdWithDetails(@Param("produitId") Long produitId);

    @Query("SELECT sf FROM StockFournisseur sf " +
            "JOIN FETCH sf.produit p " +
            "JOIN FETCH sf.fournisseur f " +
            "ORDER BY sf.id")
    List<StockFournisseur> findAllWithDetails();
}