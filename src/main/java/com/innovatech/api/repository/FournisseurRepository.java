package com.innovatech.api.repository;

import com.innovatech.api.entity.Fournisseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {

    // -----------------------------------------
    // Unicité par user
    // -----------------------------------------
    Optional<Fournisseur> findByCodeAndUserId(String code, Long userId);
    Optional<Fournisseur> findByEmailAndUserId(String email, Long userId);
    boolean existsByCodeAndUserId(String code, Long userId);
    boolean existsByEmailAndUserId(String email, Long userId);

    // -----------------------------------------
    // Par user
    // -----------------------------------------
    List<Fournisseur> findByUserId(Long userId);
    List<Fournisseur> findByUserIdAndActifTrue(Long userId);
    List<Fournisseur> findByUserIdAndPaysIgnoreCase(Long userId, String pays);
    List<Fournisseur> findByUserIdAndNomContainingIgnoreCase(Long userId, String nom);

    // -----------------------------------------
    // Avec JOIN FETCH (évite N+1)
    // -----------------------------------------
    @Query("SELECT f FROM Fournisseur f LEFT JOIN FETCH f.user WHERE f.id = :id")
    Optional<Fournisseur> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT f FROM Fournisseur f LEFT JOIN FETCH f.user " +
            "WHERE f.id = :id AND f.user.id = :userId")
    Optional<Fournisseur> findByIdAndUserIdWithUser(@Param("id") Long id,
                                                    @Param("userId") Long userId);

    @Query("SELECT f FROM Fournisseur f LEFT JOIN FETCH f.user ORDER BY f.id")
    List<Fournisseur> findAllWithUser();

    @Query("SELECT f FROM Fournisseur f LEFT JOIN FETCH f.user " +
            "WHERE f.user.id = :userId ORDER BY f.id")
    List<Fournisseur> findByUserIdWithUser(@Param("userId") Long userId);

    @Query("SELECT f FROM Fournisseur f LEFT JOIN FETCH f.user " +
            "WHERE f.user.id = :userId AND f.actif = true ORDER BY f.id")
    List<Fournisseur> findByUserIdAndActifTrueWithUser(@Param("userId") Long userId);

    @Query("SELECT f FROM Fournisseur f LEFT JOIN FETCH f.user " +
            "WHERE f.user.id = :userId AND LOWER(f.nom) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Fournisseur> searchByUserId(@Param("userId") Long userId,
                                     @Param("query") String query);

    @Query("SELECT f FROM Fournisseur f LEFT JOIN FETCH f.user " +
            "WHERE f.user.id = :userId AND LOWER(f.pays) = LOWER(:pays)")
    List<Fournisseur> findByUserIdAndPays(@Param("userId") Long userId,
                                          @Param("pays") String pays);
}