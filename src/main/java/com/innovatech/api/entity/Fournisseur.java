package com.innovatech.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Fournisseur auprès duquel InnovaTech achète des produits.
 */
@Entity
@Table(
        name = "fournisseurs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fournisseur_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_fournisseur_email", columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_fournisseur_nom", columnList = "nom"),
                @Index(name = "idx_fournisseur_pays", columnList = "pays")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Fournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Code unique du fournisseur (ex: FOUR-001).
     */
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "nom", nullable = false, length = 150)
    private String nom;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "telephone", length = 20)
    private String telephone;

    @Column(name = "adresse", length = 255)
    private String adresse;

    @Column(name = "ville", length = 100)
    private String ville;

    @Column(name = "pays", length = 100)
    private String pays;

    @Column(name = "siret", length = 14)
    private String siret;

    /**
     * Nom de la personne à contacter chez le fournisseur.
     */
    @Column(name = "contact_nom", length = 150)
    private String contactNom;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private boolean actif = true;

    @CreatedDate
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @LastModifiedDate
    @Column(name = "date_modification")
    private LocalDateTime dateModification;
}