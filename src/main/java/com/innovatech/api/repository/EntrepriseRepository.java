package com.innovatech.api.repository;

import com.innovatech.api.entity.Entreprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntrepriseRepository extends JpaRepository<Entreprise, Long> {

    Optional<Entreprise> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT e FROM Entreprise e LEFT JOIN FETCH e.owner WHERE e.id = :id")
    Optional<Entreprise> findByIdWithOwner(@Param("id") Long id);

    @Query("SELECT e FROM Entreprise e LEFT JOIN FETCH e.owner WHERE e.owner.id = :ownerId")
    Optional<Entreprise> findByOwnerId(@Param("ownerId") Long ownerId);
}