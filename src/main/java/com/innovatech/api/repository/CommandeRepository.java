package com.innovatech.api.repository;

import com.innovatech.api.entity.Commande;
import com.innovatech.api.entity.StatutCommande;
import org.springframework.data.domain.Pageable; // ⚠️ IMPORT CORRIGÉ
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {

    // -----------------------------------------
    // Numérotation par boutique
    // -----------------------------------------

    @Query("SELECT COUNT(c) FROM Commande c " +
            "WHERE YEAR(c.dateCommande) = :annee " +
            "AND c.entreprise.id = :entrepriseId " +
            "AND c.user.id = :userId")
    long countByAnneeAndEntrepriseIdAndUserId(
            @Param("annee") int annee,
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    // -----------------------------------------
    // Scoped par entreprise (ADMIN)
    // -----------------------------------------

    @Query("SELECT DISTINCT c FROM Commande c " +
            "LEFT JOIN FETCH c.client " +
            "LEFT JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.entreprise " +
            "LEFT JOIN FETCH c.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE c.entreprise.id = :entrepriseId " +
            "ORDER BY c.dateCommande DESC")
    List<Commande> findByEntrepriseIdWithDetails(@Param("entrepriseId") Long entrepriseId);

    @Query("SELECT DISTINCT c FROM Commande c " +
            "LEFT JOIN FETCH c.client " +
            "LEFT JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.entreprise " +
            "LEFT JOIN FETCH c.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE c.id = :id AND c.entreprise.id = :entrepriseId")
    Optional<Commande> findByIdAndEntrepriseIdWithDetails(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId);

    // -----------------------------------------
    // Scoped par entreprise + user (boutique)
    // -----------------------------------------

    @Query("SELECT DISTINCT c FROM Commande c " +
            "LEFT JOIN FETCH c.client " +
            "LEFT JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.entreprise " +
            "LEFT JOIN FETCH c.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE c.entreprise.id = :entrepriseId " +
            "AND c.user.id = :userId " +
            "ORDER BY c.dateCommande DESC")
    List<Commande> findByEntrepriseIdAndUserIdWithDetails(
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    @Query("SELECT DISTINCT c FROM Commande c " +
            "LEFT JOIN FETCH c.client " +
            "LEFT JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.entreprise " +
            "LEFT JOIN FETCH c.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE c.id = :id " +
            "AND c.entreprise.id = :entrepriseId " +
            "AND c.user.id = :userId")
    Optional<Commande> findByIdAndEntrepriseIdAndUserIdWithDetails(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId);

    // -----------------------------------------
    // Recherche par statut
    // -----------------------------------------

    @Query("SELECT DISTINCT c FROM Commande c " +
            "LEFT JOIN FETCH c.client " +
            "LEFT JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE c.entreprise.id = :entrepriseId AND c.statut = :statut")
    List<Commande> findByEntrepriseIdAndStatutWithDetails(
            @Param("entrepriseId") Long entrepriseId,
            @Param("statut") StatutCommande statut);

    @Query("SELECT DISTINCT c FROM Commande c " +
            "LEFT JOIN FETCH c.client " +
            "LEFT JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE c.entreprise.id = :entrepriseId " +
            "AND c.user.id = :userId " +
            "AND c.statut = :statut")
    List<Commande> findByEntrepriseIdAndUserIdAndStatutWithDetails(
            @Param("entrepriseId") Long entrepriseId,
            @Param("userId") Long userId,
            @Param("statut") StatutCommande statut);

    // -----------------------------------------
    // Par client
    // -----------------------------------------

    @Query("SELECT DISTINCT c FROM Commande c " +
            "LEFT JOIN FETCH c.client " +
            "LEFT JOIN FETCH c.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE c.client.id = :clientId " +
            "ORDER BY c.dateCommande DESC")
    List<Commande> findByClientIdWithDetails(@Param("clientId") Long clientId);

    long countByClientId(Long clientId);

    // -----------------------------------------
    // Compteurs
    // -----------------------------------------

    long countByEntrepriseId(Long entrepriseId);
    long countByEntrepriseIdAndUserId(Long entrepriseId, Long userId);
    long countByEntrepriseIdAndStatut(Long entrepriseId, StatutCommande statut);
    long countByEntrepriseIdAndUserIdAndStatut(Long entrepriseId, Long userId, StatutCommande statut);

    // ============================================================
    // DASHBOARD (AVEC FILTRE DE DATE)
    // ============================================================

    @Query("SELECT COALESCE(SUM(c.totalTTC), 0) FROM Commande c " +
            "WHERE c.entreprise.id = :entrepriseId AND c.statut <> 'ANNULEE' " +
            "AND (:dateDebut IS NULL OR c.dateCommande >= :dateDebut) " +
            "AND (:dateFin IS NULL OR c.dateCommande <= :dateFin)")
    BigDecimal sumTotalTTCByEntreprise(@Param("entrepriseId") Long entrepriseId, @Param("dateDebut") LocalDateTime dateDebut, @Param("dateFin") LocalDateTime dateFin);

    @Query("SELECT COALESCE(SUM(c.totalHT), 0) FROM Commande c " +
            "WHERE c.entreprise.id = :entrepriseId AND c.statut <> 'ANNULEE' " +
            "AND (:dateDebut IS NULL OR c.dateCommande >= :dateDebut) " +
            "AND (:dateFin IS NULL OR c.dateCommande <= :dateFin)")
    BigDecimal sumTotalHTByEntreprise(@Param("entrepriseId") Long entrepriseId, @Param("dateDebut") LocalDateTime dateDebut, @Param("dateFin") LocalDateTime dateFin);

    @Query("SELECT COALESCE(SUM((l.prixUnitaire - p.prixAchat) * l.quantite), 0) FROM Commande c JOIN c.lignes l JOIN l.produit p " +
            "WHERE c.entreprise.id = :entrepriseId AND c.statut <> 'ANNULEE' " +
            "AND (:dateDebut IS NULL OR c.dateCommande >= :dateDebut) " +
            "AND (:dateFin IS NULL OR c.dateCommande <= :dateFin)")
    BigDecimal sumMargeByEntreprise(@Param("entrepriseId") Long entrepriseId, @Param("dateDebut") LocalDateTime dateDebut, @Param("dateFin") LocalDateTime dateFin);

    @Query("SELECT COALESCE(SUM(c.totalTTC), 0) FROM Commande c " +
            "WHERE c.entreprise.id = :entrepriseId AND c.user.id = :shopOwnerId AND c.statut <> 'ANNULEE' " +
            "AND (:dateDebut IS NULL OR c.dateCommande >= :dateDebut) " +
            "AND (:dateFin IS NULL OR c.dateCommande <= :dateFin)")
    BigDecimal sumTotalTTCByBoutique(@Param("entrepriseId") Long entrepriseId, @Param("shopOwnerId") Long shopOwnerId, @Param("dateDebut") LocalDateTime dateDebut, @Param("dateFin") LocalDateTime dateFin);

    @Query("SELECT COALESCE(SUM(c.totalHT), 0) FROM Commande c " +
            "WHERE c.entreprise.id = :entrepriseId AND c.user.id = :shopOwnerId AND c.statut <> 'ANNULEE' " +
            "AND (:dateDebut IS NULL OR c.dateCommande >= :dateDebut) " +
            "AND (:dateFin IS NULL OR c.dateCommande <= :dateFin)")
    BigDecimal sumTotalHTByBoutique(@Param("entrepriseId") Long entrepriseId, @Param("shopOwnerId") Long shopOwnerId, @Param("dateDebut") LocalDateTime dateDebut, @Param("dateFin") LocalDateTime dateFin);

    @Query("SELECT COALESCE(SUM((l.prixUnitaire - p.prixAchat) * l.quantite), 0) FROM Commande c JOIN c.lignes l JOIN l.produit p " +
            "WHERE c.entreprise.id = :entrepriseId AND c.user.id = :shopOwnerId AND c.statut <> 'ANNULEE' " +
            "AND (:dateDebut IS NULL OR c.dateCommande >= :dateDebut) " +
            "AND (:dateFin IS NULL OR c.dateCommande <= :dateFin)")
    BigDecimal sumMargeByBoutique(@Param("entrepriseId") Long entrepriseId, @Param("shopOwnerId") Long shopOwnerId, @Param("dateDebut") LocalDateTime dateDebut, @Param("dateFin") LocalDateTime dateFin);

    @Query("SELECT COUNT(c) FROM Commande c " +
            "WHERE c.entreprise.id = :entrepriseId AND c.statut = :statut " +
            "AND (:dateDebut IS NULL OR c.dateCommande >= :dateDebut) " +
            "AND (:dateFin IS NULL OR c.dateCommande <= :dateFin)")
    long countByEntrepriseIdAndStatutDate(@Param("entrepriseId") Long entrepriseId, @Param("statut") StatutCommande statut, @Param("dateDebut") LocalDateTime dateDebut, @Param("dateFin") LocalDateTime dateFin);

    @Query("SELECT COUNT(c) FROM Commande c " +
            "WHERE c.entreprise.id = :entrepriseId AND c.user.id = :userId AND c.statut = :statut " +
            "AND (:dateDebut IS NULL OR c.dateCommande >= :dateDebut) " +
            "AND (:dateFin IS NULL OR c.dateCommande <= :dateFin)")
    long countByEntrepriseIdAndUserIdAndStatutDate(@Param("entrepriseId") Long entrepriseId, @Param("userId") Long userId, @Param("statut") StatutCommande statut, @Param("dateDebut") LocalDateTime dateDebut, @Param("dateFin") LocalDateTime dateFin);

    @Query("SELECT p.id, p.reference, p.nom, SUM(l.quantite), SUM(l.sousTotal) " +
            "FROM Commande c JOIN c.lignes l JOIN l.produit p " +
            "WHERE c.entreprise.id = :entrepriseId AND c.statut <> 'ANNULEE' " +
            "AND (:shopOwnerId IS NULL OR c.user.id = :shopOwnerId) " +
            "AND (:dateDebut IS NULL OR c.dateCommande >= :dateDebut) " +
            "AND (:dateFin IS NULL OR c.dateCommande <= :dateFin) " +
            "GROUP BY p.id, p.reference, p.nom " +
            "ORDER BY SUM(l.quantite) DESC")
    List<Object[]> findTopProduits(@Param("entrepriseId") Long entrepriseId, @Param("shopOwnerId") Long shopOwnerId, @Param("dateDebut") LocalDateTime dateDebut, @Param("dateFin") LocalDateTime dateFin, Pageable pageable);

    @Query("SELECT cl.id, cl.nom, cl.prenom, cl.email, COUNT(c.id), SUM(c.totalTTC) " +
            "FROM Commande c JOIN c.client cl " +
            "WHERE c.entreprise.id = :entrepriseId AND c.statut <> 'ANNULEE' " +
            "AND (:shopOwnerId IS NULL OR c.user.id = :shopOwnerId) " +
            "AND (:dateDebut IS NULL OR c.dateCommande >= :dateDebut) " +
            "AND (:dateFin IS NULL OR c.dateCommande <= :dateFin) " +
            "GROUP BY cl.id, cl.nom, cl.prenom, cl.email " +
            "ORDER BY SUM(c.totalTTC) DESC")
    List<Object[]> findTopClients(@Param("entrepriseId") Long entrepriseId, @Param("shopOwnerId") Long shopOwnerId, @Param("dateDebut") LocalDateTime dateDebut, @Param("dateFin") LocalDateTime dateFin, Pageable pageable);

    @Query("SELECT YEAR(c.dateCommande), MONTH(c.dateCommande), SUM(c.totalTTC), COUNT(c.id) " +
            "FROM Commande c " +
            "WHERE c.entreprise.id = :entrepriseId AND c.statut <> 'ANNULEE' " +
            "AND (:shopOwnerId IS NULL OR c.user.id = :shopOwnerId) " +
            "AND (:dateDebut IS NULL OR c.dateCommande >= :dateDebut) " +
            "AND (:dateFin IS NULL OR c.dateCommande <= :dateFin) " +
            "GROUP BY YEAR(c.dateCommande), MONTH(c.dateCommande) " +
            "ORDER BY YEAR(c.dateCommande) ASC, MONTH(c.dateCommande) ASC")
    List<Object[]> findChiffreAffairesMensuel(@Param("entrepriseId") Long entrepriseId, @Param("shopOwnerId") Long shopOwnerId, @Param("dateDebut") LocalDateTime dateDebut, @Param("dateFin") LocalDateTime dateFin);
}