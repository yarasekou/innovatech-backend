package com.innovatech.api.repository;

import com.innovatech.api.entity.CommandeAchat;
import com.innovatech.api.entity.StatutAchat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommandeAchatRepository extends JpaRepository<CommandeAchat, Long> {

    // -----------------------------------------
    // Numérotation par boutique
    // -----------------------------------------

    @Query("SELECT COUNT(c) FROM CommandeAchat c " +
            "WHERE YEAR(c.dateCommande) = :annee " +
            "AND c.entreprise.id = :entrepriseId " +
            "AND c.user.id = :userId")
    long countByAnneeAndEntrepriseIdAndUserId(
            @Param("annee") int annee,
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    // -----------------------------------------
    // Par entreprise (ADMIN)
    // -----------------------------------------

    @Query("SELECT DISTINCT c FROM CommandeAchat c " +
            "JOIN FETCH c.fournisseur " +
            "JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE c.entreprise.id = :entrepriseId " +
            "ORDER BY c.dateCommande DESC")
    List<CommandeAchat> findByEntrepriseIdWithDetails(@Param("entrepriseId") Long entrepriseId);

    @Query("SELECT DISTINCT c FROM CommandeAchat c " +
            "JOIN FETCH c.fournisseur " +
            "JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE c.id = :id AND c.entreprise.id = :entrepriseId")
    Optional<CommandeAchat> findByIdAndEntrepriseIdWithDetails(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId);

    // -----------------------------------------
    // Par entreprise + user (boutique)
    // -----------------------------------------

    @Query("SELECT DISTINCT c FROM CommandeAchat c " +
            "JOIN FETCH c.fournisseur " +
            "JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE c.entreprise.id = :entrepriseId " +
            "AND c.user.id = :userId " +
            "ORDER BY c.dateCommande DESC")
    List<CommandeAchat> findByEntrepriseIdAndUserIdWithDetails(
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    @Query("SELECT DISTINCT c FROM CommandeAchat c " +
            "JOIN FETCH c.fournisseur " +
            "JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE c.id = :id " +
            "AND c.entreprise.id = :entrepriseId " +
            "AND c.user.id = :userId")
    Optional<CommandeAchat> findByIdAndEntrepriseIdAndUserIdWithDetails(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    // -----------------------------------------
    // Par statut
    // -----------------------------------------

    @Query("SELECT DISTINCT c FROM CommandeAchat c " +
            "JOIN FETCH c.fournisseur " +
            "JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE c.entreprise.id = :entrepriseId " +
            "AND c.statut = :statut")
    List<CommandeAchat> findByEntrepriseIdAndStatutWithDetails(
            @Param("entrepriseId") Long entrepriseId,
            @Param("statut") StatutAchat statut);

    @Query("SELECT DISTINCT c FROM CommandeAchat c " +
            "JOIN FETCH c.fournisseur " +
            "JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE c.entreprise.id = :entrepriseId " +
            "AND c.user.id = :userId " +
            "AND c.statut = :statut")
    List<CommandeAchat> findByEntrepriseIdAndUserIdAndStatutWithDetails(
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId,
            @Param("statut") StatutAchat statut);

    // -----------------------------------------
    // Par fournisseur
    // -----------------------------------------

    @Query("SELECT DISTINCT c FROM CommandeAchat c " +
            "JOIN FETCH c.fournisseur " +
            "JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE c.fournisseur.id = :fournisseurId " +
            "ORDER BY c.dateCommande DESC")
    List<CommandeAchat> findByFournisseurIdWithDetails(@Param("fournisseurId") Long fournisseurId);

    // -----------------------------------------
    // Détails
    // -----------------------------------------

    @Query("SELECT DISTINCT c FROM CommandeAchat c " +
            "JOIN FETCH c.fournisseur " +
            "JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE c.id = :id")
    Optional<CommandeAchat> findByIdWithDetails(@Param("id") Long id);

    // -----------------------------------------
    // Compteurs
    // -----------------------------------------

    long countByFournisseurId(Long fournisseurId);
    long countByEntrepriseId(Long entrepriseId);
    long countByEntrepriseIdAndUserId(Long entrepriseId, Long userId);
}