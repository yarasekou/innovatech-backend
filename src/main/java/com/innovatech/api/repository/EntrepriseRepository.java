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

    Optional<Entreprise> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("SELECT e FROM Entreprise e LEFT JOIN FETCH e.user WHERE e.id = :id")
    Optional<Entreprise> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT e FROM Entreprise e LEFT JOIN FETCH e.user WHERE e.user.id = :userId")
    Optional<Entreprise> findByUserIdWithUser(@Param("userId") Long userId);
}