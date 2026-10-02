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

    Optional<Retour> findByNumero(String numero);

    boolean existsByNumero(String numero);

    @Query("SELECT COUNT(r) FROM Retour r WHERE YEAR(r.dateDemande) = :annee")
    long countByAnnee(@Param("annee") int annee);

    // -----------------------------------------
    // Par user
    // -----------------------------------------
    List<Retour> findByUserId(Long userId);
    List<Retour> findByUserIdAndStatut(Long userId, StatutRetour statut);
    List<Retour> findByCommandeId(Long commandeId);
    List<Retour> findByClientId(Long clientId);

    // -----------------------------------------
    // Avec JOIN FETCH
    // -----------------------------------------
    @Query("SELECT DISTINCT r FROM Retour r " +
            "JOIN FETCH r.commande " +
            "JOIN FETCH r.client " +
            "LEFT JOIN FETCH r.user " +
            "LEFT JOIN FETCH r.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE r.id = :id")
    Optional<Retour> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT r FROM Retour r " +
            "JOIN FETCH r.commande " +
            "JOIN FETCH r.client " +
            "LEFT JOIN FETCH r.user " +
            "LEFT JOIN FETCH r.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "ORDER BY r.dateDemande DESC")
    List<Retour> findAllWithDetails();

    @Query("SELECT DISTINCT r FROM Retour r " +
            "JOIN FETCH r.commande " +
            "JOIN FETCH r.client " +
            "LEFT JOIN FETCH r.user " +
            "LEFT JOIN FETCH r.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE r.user.id = :userId " +
            "ORDER BY r.dateDemande DESC")
    List<Retour> findByUserIdWithDetails(@Param("userId") Long userId);

    @Query("SELECT DISTINCT r FROM Retour r " +
            "JOIN FETCH r.commande " +
            "JOIN FETCH r.client " +
            "LEFT JOIN FETCH r.user " +
            "LEFT JOIN FETCH r.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE r.id = :id AND r.user.id = :userId")
    Optional<Retour> findByIdAndUserIdWithDetails(@Param("id") Long id,
                                                  @Param("userId") Long userId);

    @Query("SELECT DISTINCT r FROM Retour r " +
            "JOIN FETCH r.commande " +
            "JOIN FETCH r.client " +
            "LEFT JOIN FETCH r.user " +
            "LEFT JOIN FETCH r.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE r.user.id = :userId AND r.statut = :statut " +
            "ORDER BY r.dateDemande DESC")
    List<Retour> findByUserIdAndStatutWithDetails(@Param("userId") Long userId,
                                                  @Param("statut") StatutRetour statut);
}