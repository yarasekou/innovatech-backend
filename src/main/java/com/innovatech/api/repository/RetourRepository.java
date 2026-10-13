package com.innovatech.api.repository;

import com.innovatech.api.entity.Retour;
import com.innovatech.api.entity.StatutRetour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RetourRepository extends JpaRepository<Retour, Long> {

    @Query("SELECT COUNT(r) FROM Retour r " +
            "WHERE YEAR(r.dateDemande) = :annee " +
            "AND r.entreprise.id = :entrepriseId " +
            "AND r.user.id = :userId")
    long countByAnneeAndEntrepriseIdAndUserId(
            @Param("annee") int annee,
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    // -----------------------------------------
    // Par commande / client
    // -----------------------------------------

    List<Retour> findByCommandeId(Long commandeId);
    List<Retour> findByClientId(Long clientId);

    // -----------------------------------------
    // Par entreprise (ADMIN)
    // -----------------------------------------

    @Query("SELECT DISTINCT r FROM Retour r " +
            "JOIN FETCH r.commande " +
            "JOIN FETCH r.client " +
            "JOIN FETCH r.user " +
            "LEFT JOIN FETCH r.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE r.entreprise.id = :entrepriseId " +
            "ORDER BY r.dateDemande DESC")
    List<Retour> findByEntrepriseIdWithDetails(@Param("entrepriseId") Long entrepriseId);

    @Query("SELECT DISTINCT r FROM Retour r " +
            "JOIN FETCH r.commande " +
            "JOIN FETCH r.client " +
            "JOIN FETCH r.user " +
            "LEFT JOIN FETCH r.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE r.id = :id AND r.entreprise.id = :entrepriseId")
    Optional<Retour> findByIdAndEntrepriseIdWithDetails(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId);

    // -----------------------------------------
    // Par entreprise + user (boutique)
    // -----------------------------------------

    @Query("SELECT DISTINCT r FROM Retour r " +
            "JOIN FETCH r.commande " +
            "JOIN FETCH r.client " +
            "JOIN FETCH r.user " +
            "LEFT JOIN FETCH r.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE r.entreprise.id = :entrepriseId " +
            "AND r.user.id = :userId " +
            "ORDER BY r.dateDemande DESC")
    List<Retour> findByEntrepriseIdAndUserIdWithDetails(
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    @Query("SELECT DISTINCT r FROM Retour r " +
            "JOIN FETCH r.commande " +
            "JOIN FETCH r.client " +
            "JOIN FETCH r.user " +
            "LEFT JOIN FETCH r.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE r.id = :id " +
            "AND r.entreprise.id = :entrepriseId " +
            "AND r.user.id = :userId")
    Optional<Retour> findByIdAndEntrepriseIdAndUserIdWithDetails(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    // -----------------------------------------
    // Par statut
    // -----------------------------------------

    @Query("SELECT DISTINCT r FROM Retour r " +
            "JOIN FETCH r.commande " +
            "JOIN FETCH r.client " +
            "JOIN FETCH r.user " +
            "LEFT JOIN FETCH r.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE r.entreprise.id = :entrepriseId " +
            "AND r.statut = :statut " +
            "ORDER BY r.dateDemande DESC")
    List<Retour> findByEntrepriseIdAndStatutWithDetails(
            @Param("entrepriseId") Long entrepriseId,
            @Param("statut") StatutRetour statut);

    @Query("SELECT DISTINCT r FROM Retour r " +
            "JOIN FETCH r.commande " +
            "JOIN FETCH r.client " +
            "JOIN FETCH r.user " +
            "LEFT JOIN FETCH r.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE r.entreprise.id = :entrepriseId " +
            "AND r.user.id = :userId " +
            "AND r.statut = :statut " +
            "ORDER BY r.dateDemande DESC")
    List<Retour> findByEntrepriseIdAndUserIdAndStatutWithDetails(
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId,
            @Param("statut") StatutRetour statut);

    // -----------------------------------------
    // Détails génériques
    // -----------------------------------------

    @Query("SELECT DISTINCT r FROM Retour r " +
            "JOIN FETCH r.commande " +
            "JOIN FETCH r.client " +
            "JOIN FETCH r.user " +
            "LEFT JOIN FETCH r.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE r.id = :id")
    Optional<Retour> findByIdWithDetails(@Param("id") Long id);

    long countByEntrepriseId(Long entrepriseId);
    long countByEntrepriseIdAndUserId(Long entrepriseId, Long userId);
}