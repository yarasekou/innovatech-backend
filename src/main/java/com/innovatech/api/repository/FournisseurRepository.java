package com.innovatech.api.repository;

import com.innovatech.api.entity.Fournisseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {

    Optional<Fournisseur> findByCode(String code);

    Optional<Fournisseur> findByEmail(String email);

    boolean existsByCode(String code);

    boolean existsByEmail(String email);

    List<Fournisseur> findByActifTrue();

    List<Fournisseur> findByPaysIgnoreCase(String pays);

    List<Fournisseur> findByNomContainingIgnoreCase(String nom);
}