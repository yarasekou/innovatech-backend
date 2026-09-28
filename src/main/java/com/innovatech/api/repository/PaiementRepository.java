package com.innovatech.api.repository;

import com.innovatech.api.entity.Paiement;
import com.innovatech.api.entity.StatutPaiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    Optional<Paiement> findByNumero(String numero);

    boolean existsByNumero(String numero);

    List<Paiement> findByCommandeIdOrderByDatePaiementDesc(Long commandeId);

    List<Paiement> findByStatut(StatutPaiement statut);

    long countByCommandeId(Long commandeId);

    /**
     * Compte les paiements d'une année (pour numérotation).
     */
    @Query("SELECT COUNT(p) FROM Paiement p WHERE YEAR(p.datePaiement) = :annee")
    long countByAnnee(@Param("annee") int annee);

    /**
     * Somme des paiements VALIDES pour une commande.
     */
    @Query("SELECT COALESCE(SUM(p.montant), 0) FROM Paiement p " +
            "WHERE p.commande.id = :commandeId AND p.statut = 'VALIDE'")
    BigDecimal sumMontantValideByCommandeId(@Param("commandeId") Long commandeId);

    /**
     * Charge un paiement avec sa commande (pour éviter le N+1).
     */
    @Query("SELECT p FROM Paiement p JOIN FETCH p.commande WHERE p.id = :id")
    Optional<Paiement> findByIdWithCommande(@Param("id") Long id);

    @Query("SELECT p FROM Paiement p JOIN FETCH p.commande ORDER BY p.datePaiement DESC")
    List<Paiement> findAllWithCommande();

    @Query("SELECT p FROM Paiement p JOIN FETCH p.commande " +
            "WHERE p.commande.id = :commandeId ORDER BY p.datePaiement DESC")
    List<Paiement> findByCommandeIdWithCommande(@Param("commandeId") Long commandeId);
}