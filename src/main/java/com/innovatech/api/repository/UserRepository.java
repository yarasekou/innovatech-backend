package com.innovatech.api.repository;

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

    /**
     * Liste les SELLERs (employés) d'un patron.
     */
    List<User> findByPatronId(Long patronId);

    /**
     * Compte les employés d'un patron.
     */
    long countByPatronId(Long patronId);

    /**
     * Charge un user avec son patron.
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.patron WHERE u.id = :id")
    Optional<User> findByIdWithPatron(@Param("id") Long id);

    /**
     * Charge un user avec ses employés.
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.employes WHERE u.id = :id")
    Optional<User> findByIdWithEmployes(@Param("id") Long id);
}