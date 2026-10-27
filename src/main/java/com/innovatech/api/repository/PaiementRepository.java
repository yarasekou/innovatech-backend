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

    // -----------------------------------------
    // Numérotation par boutique
    // -----------------------------------------

    @Query("SELECT COUNT(p) FROM Paiement p " +
            "WHERE YEAR(p.datePaiement) = :annee " +
            "AND p.entreprise.id = :entrepriseId " +
            "AND p.user.id = :userId")
    long countByAnneeAndEntrepriseIdAndUserId(
            @Param("annee") int annee,
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    // -----------------------------------------
    // Somme des paiements VALIDES pour une commande
    // -----------------------------------------

    @Query("SELECT COALESCE(SUM(p.montant), 0) FROM Paiement p " +
            "WHERE p.commande.id = :commandeId AND p.statut = 'VALIDE'")
    BigDecimal sumMontantValideByCommandeId(@Param("commandeId") Long commandeId);

    // -----------------------------------------
    // Par entreprise (ADMIN)
    // -----------------------------------------

    @Query("SELECT p FROM Paiement p " +
            "JOIN FETCH p.commande " +
            "JOIN FETCH p.user " +
            "JOIN FETCH p.entreprise " +
            "WHERE p.entreprise.id = :entrepriseId " +
            "ORDER BY p.datePaiement DESC")
    List<Paiement> findByEntrepriseIdWithDetails(@Param("entrepriseId") Long entrepriseId);

    @Query("SELECT p FROM Paiement p " +
            "JOIN FETCH p.commande " +
            "JOIN FETCH p.user " +
            "JOIN FETCH p.entreprise " +
            "WHERE p.id = :id AND p.entreprise.id = :entrepriseId")
    Optional<Paiement> findByIdAndEntrepriseIdWithDetails(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId);

    // -----------------------------------------
    // Par entreprise + user (boutique)
    // -----------------------------------------

    @Query("SELECT p FROM Paiement p " +
            "JOIN FETCH p.commande " +
            "JOIN FETCH p.user " +
            "JOIN FETCH p.entreprise " +
            "WHERE p.entreprise.id = :entrepriseId " +
            "AND p.user.id = :userId " +
            "ORDER BY p.datePaiement DESC")
    List<Paiement> findByEntrepriseIdAndUserIdWithDetails(
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    @Query("SELECT p FROM Paiement p " +
            "JOIN FETCH p.commande " +
            "JOIN FETCH p.user " +
            "JOIN FETCH p.entreprise " +
            "WHERE p.id = :id " +
            "AND p.entreprise.id = :entrepriseId " +
            "AND p.user.id = :userId")
    Optional<Paiement> findByIdAndEntrepriseIdAndUserIdWithDetails(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    // -----------------------------------------
    // Par commande
    // -----------------------------------------

    @Query("SELECT p FROM Paiement p " +
            "JOIN FETCH p.commande " +
            "JOIN FETCH p.user " +
            "WHERE p.commande.id = :commandeId " +
            "ORDER BY p.datePaiement DESC")
    List<Paiement> findByCommandeIdWithDetails(@Param("commandeId") Long commandeId);

    // -----------------------------------------
    // Par statut
    // -----------------------------------------

    @Query("SELECT p FROM Paiement p " +
            "JOIN FETCH p.commande " +
            "JOIN FETCH p.user " +
            "WHERE p.entreprise.id = :entrepriseId " +
            "AND p.statut = :statut")
    List<Paiement> findByEntrepriseIdAndStatutWithDetails(
            @Param("entrepriseId") Long entrepriseId,
            @Param("statut") StatutPaiement statut);

    @Query("SELECT p FROM Paiement p " +
            "JOIN FETCH p.commande " +
            "JOIN FETCH p.user " +
            "WHERE p.entreprise.id = :entrepriseId " +
            "AND p.user.id = :userId " +
            "AND p.statut = :statut")
    List<Paiement> findByEntrepriseIdAndUserIdAndStatutWithDetails(
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId,
            @Param("statut") StatutPaiement statut);

    // -----------------------------------------
    // Compteurs
    // -----------------------------------------

    long countByEntrepriseId(Long entrepriseId);
    long countByEntrepriseIdAndUserId(Long entrepriseId, Long userId);
    long countByCommandeId(Long commandeId);

    @Query("SELECT p FROM Paiement p " +
            "JOIN FETCH p.commandeAchat " +
            "JOIN FETCH p.user " +
            "WHERE p.commandeAchat.id = :commandeAchatId " +
            "ORDER BY p.datePaiement DESC")
    List<Paiement> findByCommandeAchatIdWithDetails(@Param("commandeAchatId") Long commandeAchatId);
}