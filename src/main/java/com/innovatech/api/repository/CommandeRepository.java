package com.innovatech.api.repository;

import com.innovatech.api.entity.Commande;
import com.innovatech.api.entity.StatutCommande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {

    // Méthodes existantes
    Optional<Commande> findByNumero(String numero);

    boolean existsByNumero(String numero);

    List<Commande> findByClientIdOrderByDateCommandeDesc(Long clientId);

    List<Commande> findByStatut(StatutCommande statut);

    long countByClientId(Long clientId);

    @Query("SELECT COUNT(c) FROM Commande c WHERE YEAR(c.dateCommande) = :annee")
    long countByAnnee(@Param("annee") int annee);

    // ---------------------------------------------------
    // NOUVELLES MÉTHODES avec JOIN FETCH (évite le N+1)
    // ---------------------------------------------------

    @Query("SELECT c FROM Commande c JOIN FETCH c.client WHERE c.id = :id")
    Optional<Commande> findByIdWithClient(@Param("id") Long id);

    @Query("SELECT c FROM Commande c JOIN FETCH c.client ORDER BY c.dateCommande DESC")
    List<Commande> findAllWithClient();

    @Query("SELECT c FROM Commande c JOIN FETCH c.client WHERE c.numero = :numero")
    Optional<Commande> findByNumeroWithClient(@Param("numero") String numero);

    @Query("SELECT c FROM Commande c JOIN FETCH c.client WHERE c.client.id = :clientId ORDER BY c.dateCommande DESC")
    List<Commande> findByClientIdWithClient(@Param("clientId") Long clientId);

    @Query("SELECT c FROM Commande c JOIN FETCH c.client WHERE c.statut = :statut")
    List<Commande> findByStatutWithClient(@Param("statut") StatutCommande statut);
}