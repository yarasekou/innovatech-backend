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

    Optional<CommandeAchat> findByNumero(String numero);

    boolean existsByNumero(String numero);

    long countByFournisseurId(Long fournisseurId);

    @Query("SELECT COUNT(c) FROM CommandeAchat c WHERE YEAR(c.dateCommande) = :annee")
    long countByAnnee(@Param("annee") int annee);

    // -------------------------------------------------
    // Avec JOIN FETCH (éviter le N+1)
    // -------------------------------------------------

    @Query("SELECT DISTINCT c FROM CommandeAchat c " +
            "JOIN FETCH c.fournisseur " +
            "LEFT JOIN FETCH c.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE c.id = :id")
    Optional<CommandeAchat> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT c FROM CommandeAchat c " +
            "JOIN FETCH c.fournisseur " +
            "LEFT JOIN FETCH c.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "ORDER BY c.dateCommande DESC")
    List<CommandeAchat> findAllWithDetails();

    @Query("SELECT DISTINCT c FROM CommandeAchat c " +
            "JOIN FETCH c.fournisseur " +
            "LEFT JOIN FETCH c.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE c.numero = :numero")
    Optional<CommandeAchat> findByNumeroWithDetails(@Param("numero") String numero);

    @Query("SELECT DISTINCT c FROM CommandeAchat c " +
            "JOIN FETCH c.fournisseur " +
            "LEFT JOIN FETCH c.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE c.fournisseur.id = :fournisseurId " +
            "ORDER BY c.dateCommande DESC")
    List<CommandeAchat> findByFournisseurIdWithDetails(@Param("fournisseurId") Long fournisseurId);

    @Query("SELECT DISTINCT c FROM CommandeAchat c " +
            "JOIN FETCH c.fournisseur " +
            "LEFT JOIN FETCH c.lignes l " +
            "LEFT JOIN FETCH l.produit " +
            "WHERE c.statut = :statut")
    List<CommandeAchat> findByStatutWithDetails(@Param("statut") StatutAchat statut);
}