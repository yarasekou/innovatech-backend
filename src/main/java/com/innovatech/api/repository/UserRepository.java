package com.innovatech.api.repository;

import com.innovatech.api.entity.Role;
import com.innovatech.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ============================================================
    // Recherche par email / username (exclut les supprimés)
    // ============================================================

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.supprime = false")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.username = :username AND u.supprime = false")
    Optional<User> findByUsername(@Param("username") String username);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
            "FROM User u WHERE u.email = :email AND u.supprime = false")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
            "FROM User u WHERE u.username = :username AND u.supprime = false")
    boolean existsByUsername(@Param("username") String username);

    // ============================================================
    // Hiérarchie SELLER → USER
    // ============================================================

    @Query("SELECT u FROM User u WHERE u.patron.id = :patronId AND u.supprime = false")
    List<User> findByPatronId(@Param("patronId") Long patronId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.patron.id = :patronId AND u.supprime = false")
    long countByPatronId(@Param("patronId") Long patronId);

    // ============================================================
    // Par entreprise et rôle
    // ============================================================

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.entreprise " +
            "LEFT JOIN FETCH u.patron " +
            "LEFT JOIN FETCH u.employes " +
            "JOIN u.roles r " +
            "WHERE u.entreprise.id = :entrepriseId " +
            "AND r = :role " +
            "AND u.supprime = false " +
            "ORDER BY u.id")
    List<User> findByEntrepriseIdAndRole(
            @Param("entrepriseId") Long entrepriseId,
            @Param("role") Role role);

    // ============================================================
    // Tous les users avec entreprise (SUPER_ADMIN) — sans supprimés
    // ============================================================

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.entreprise " +
            "WHERE u.supprime = false " +
            "ORDER BY u.id")
    List<User> findAllWithEntreprise();

    // ============================================================
    // Par rôle (exclut les supprimés)
    // ============================================================

    @Query("SELECT DISTINCT u FROM User u " +
            "JOIN u.roles r " +
            "WHERE r = :role AND u.supprime = false " +
            "ORDER BY u.id")
    List<User> findAllByRole(@Param("role") Role role);

    // ============================================================
    // Avec JOIN FETCH
    // ============================================================

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.patron " +
            "WHERE u.id = :id AND u.supprime = false")
    Optional<User> findByIdWithPatron(@Param("id") Long id);

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.employes " +
            "WHERE u.id = :id AND u.supprime = false")
    Optional<User> findByIdWithEmployes(@Param("id") Long id);

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.entreprise " +
            "LEFT JOIN FETCH u.patron " +
            "WHERE u.id = :id AND u.supprime = false")
    Optional<User> findByIdWithDetails(@Param("id") Long id);

    // ============================================================
    // Par défaut : exclut les supprimés
    // ============================================================

    @Query("SELECT u FROM User u WHERE u.supprime = false ORDER BY u.id")
    List<User> findAllActifs();
}