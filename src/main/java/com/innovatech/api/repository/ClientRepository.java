package com.innovatech.api.repository;

import com.innovatech.api.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    // -----------------------------------------
    // Méthodes avec user
    // -----------------------------------------

    Optional<Client> findByEmailAndUserId(String email, Long userId);

    boolean existsByEmailAndUserId(String email, Long userId);

    List<Client> findByUserId(Long userId);

    List<Client> findByUserIdAndActifTrue(Long userId);

    // -----------------------------------------
    // Méthodes avec JOIN FETCH (évite le N+1)
    // -----------------------------------------

    @Query("SELECT c FROM Client c LEFT JOIN FETCH c.user WHERE c.id = :id")
    Optional<Client> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT c FROM Client c LEFT JOIN FETCH c.user " +
            "WHERE c.id = :id AND c.user.id = :userId")
    Optional<Client> findByIdAndUserIdWithUser(@Param("id") Long id,
                                               @Param("userId") Long userId);

    @Query("SELECT c FROM Client c LEFT JOIN FETCH c.user ORDER BY c.id")
    List<Client> findAllWithUser();

    @Query("SELECT c FROM Client c LEFT JOIN FETCH c.user " +
            "WHERE c.user.id = :userId ORDER BY c.id")
    List<Client> findByUserIdWithUser(@Param("userId") Long userId);

    @Query("SELECT c FROM Client c LEFT JOIN FETCH c.user " +
            "WHERE c.user.id = :userId AND c.actif = true ORDER BY c.id")
    List<Client> findByUserIdAndActifTrueWithUser(@Param("userId") Long userId);

    @Query("SELECT c FROM Client c LEFT JOIN FETCH c.user " +
            "WHERE c.user.id = :userId " +
            "AND (LOWER(c.nom) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(c.prenom) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Client> searchByUserId(@Param("userId") Long userId,
                                @Param("query") String query);
}