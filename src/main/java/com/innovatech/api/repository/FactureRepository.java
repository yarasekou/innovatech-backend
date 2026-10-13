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

    // -----------------------------------------
    // Numérotation
    // -----------------------------------------

    @Query("SELECT COUNT(f) FROM Facture f " +
            "WHERE YEAR(f.dateEmission) = :annee " +
            "AND f.entreprise.id = :entrepriseId " +
            "AND f.user.id = :userId")
    long countByAnneeAndEntrepriseIdAndUserId(
            @Param("annee") int annee,
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    // -----------------------------------------
    // Par commande
    // -----------------------------------------

    Optional<Facture> findByCommandeId(Long commandeId);

    boolean existsByCommandeId(Long commandeId);

    // -----------------------------------------
    // Par entreprise (ADMIN)
    // -----------------------------------------

    @Query("SELECT f FROM Facture f " +
            "JOIN FETCH f.commande " +
            "JOIN FETCH f.client " +
            "JOIN FETCH f.user " +
            "WHERE f.entreprise.id = :entrepriseId " +
            "ORDER BY f.dateEmission DESC")
    List<Facture> findByEntrepriseIdWithDetails(@Param("entrepriseId") Long entrepriseId);

    @Query("SELECT f FROM Facture f " +
            "JOIN FETCH f.commande " +
            "JOIN FETCH f.client " +
            "JOIN FETCH f.user " +
            "WHERE f.id = :id AND f.entreprise.id = :entrepriseId")
    Optional<Facture> findByIdAndEntrepriseIdWithDetails(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId);

    // -----------------------------------------
    // Par entreprise + user (boutique)
    // -----------------------------------------

    @Query("SELECT f FROM Facture f " +
            "JOIN FETCH f.commande " +
            "JOIN FETCH f.client " +
            "JOIN FETCH f.user " +
            "WHERE f.entreprise.id = :entrepriseId " +
            "AND f.user.id = :userId " +
            "ORDER BY f.dateEmission DESC")
    List<Facture> findByEntrepriseIdAndUserIdWithDetails(
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    @Query("SELECT f FROM Facture f " +
            "JOIN FETCH f.commande " +
            "JOIN FETCH f.client " +
            "JOIN FETCH f.user " +
            "WHERE f.id = :id " +
            "AND f.entreprise.id = :entrepriseId " +
            "AND f.user.id = :userId")
    Optional<Facture> findByIdAndEntrepriseIdAndUserIdWithDetails(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    // -----------------------------------------
    // Détails génériques
    // -----------------------------------------

    @Query("SELECT f FROM Facture f " +
            "JOIN FETCH f.commande " +
            "JOIN FETCH f.client " +
            "JOIN FETCH f.user " +
            "WHERE f.id = :id")
    Optional<Facture> findByIdWithDetails(@Param("id") Long id);

    // -----------------------------------------
    // Par statut
    // -----------------------------------------

    List<Facture> findByStatut(StatutFacture statut);

    @Query("SELECT f FROM Facture f " +
            "WHERE f.statut IN ('EMISE', 'EN_RETARD') " +
            "AND f.dateEcheance < :today")
    List<Facture> findFacturesEnRetard(@Param("today") LocalDate today);

    long countByStatut(StatutFacture statut);
    long countByEntrepriseId(Long entrepriseId);
    long countByEntrepriseIdAndUserId(Long entrepriseId, Long userId);
}