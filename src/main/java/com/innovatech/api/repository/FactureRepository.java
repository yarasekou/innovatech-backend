package com.innovatech.api.repository;

import com.innovatech.api.entity.Facture;
import com.innovatech.api.entity.StatutFacture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {

    Optional<Facture> findByNumero(String numero);

    boolean existsByNumero(String numero);

    Optional<Facture> findByCommandeId(Long commandeId);

    boolean existsByCommandeId(Long commandeId);

    @Query("SELECT COUNT(f) FROM Facture f WHERE YEAR(f.dateEmission) = :annee")
    long countByAnnee(@Param("annee") int annee);

    // -----------------------------------------
    // Par statut
    // -----------------------------------------
    List<Facture> findByStatut(StatutFacture statut);

    /**
     * Factures dont la date d'échéance est dépassée et qui ne sont pas payées.
     */
    @Query("SELECT f FROM Facture f " +
            "WHERE f.statut IN ('EMISE', 'EN_RETARD') " +
            "AND f.dateEcheance < :today")
    List<Facture> findFacturesEnRetard(@Param("today") LocalDate today);

    // -----------------------------------------
    // Avec JOIN FETCH
    // -----------------------------------------
    @Query("SELECT f FROM Facture f " +
            "JOIN FETCH f.commande c " +
            "JOIN FETCH f.client " +
            "WHERE f.id = :id")
    Optional<Facture> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT f FROM Facture f " +
            "JOIN FETCH f.commande " +
            "JOIN FETCH f.client " +
            "ORDER BY f.dateEmission DESC")
    List<Facture> findAllWithDetails();

    @Query("SELECT f FROM Facture f " +
            "JOIN FETCH f.commande c " +
            "JOIN FETCH f.client " +
            "WHERE c.vendeur.id = :vendeurId " +
            "ORDER BY f.dateEmission DESC")
    List<Facture> findByVendeurIdWithDetails(@Param("vendeurId") Long vendeurId);

    @Query("SELECT f FROM Facture f " +
            "JOIN FETCH f.commande c " +
            "JOIN FETCH f.client " +
            "WHERE f.id = :id AND c.vendeur.id = :vendeurId")
    Optional<Facture> findByIdAndVendeurIdWithDetails(@Param("id") Long id,
                                                      @Param("vendeurId") Long vendeurId);

    long countByStatut(StatutFacture statut);
}