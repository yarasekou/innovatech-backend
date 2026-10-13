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

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    // -----------------------------------------
    // Hiérarchie SELLER → USER (patron)
    // -----------------------------------------

    List<User> findByPatronId(Long patronId);

    long countByPatronId(Long patronId);

    // -----------------------------------------
    // Par entreprise et rôle (utilisé par UserService)
    // -----------------------------------------

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.entreprise " +
            "LEFT JOIN FETCH u.patron " +
            "LEFT JOIN FETCH u.employes " +
            "JOIN u.roles r " +
            "WHERE u.entreprise.id = :entrepriseId " +
            "AND r = :role " +
            "ORDER BY u.id")
    List<User> findByEntrepriseIdAndRole(
            @Param("entrepriseId") Long entrepriseId,
            @Param("role") Role role);

    // -----------------------------------------
    // Avec JOIN FETCH
    // -----------------------------------------

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.patron WHERE u.id = :id")
    Optional<User> findByIdWithPatron(@Param("id") Long id);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.employes WHERE u.id = :id")
    Optional<User> findByIdWithEmployes(@Param("id") Long id);

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.entreprise " +
            "LEFT JOIN FETCH u.patron " +
            "WHERE u.id = :id")
    Optional<User> findByIdWithDetails(@Param("id") Long id);
}