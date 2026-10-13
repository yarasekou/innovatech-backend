package com.innovatech.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Client d'une boutique (USER) dans une entreprise.
 * <p>
 * Un même client (email) peut exister dans plusieurs boutiques.
 */
@Entity
@Table(
        name = "clients",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_client_email_entreprise_user",
                        columnNames = {"email", "entreprise_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_client_nom", columnList = "nom"),
                @Index(name = "idx_client_ville", columnList = "ville"),
                @Index(name = "idx_client_entreprise", columnList = "entreprise_id"),
                @Index(name = "idx_client_user", columnList = "user_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "entreprise"})
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

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

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private boolean actif = true;

    // -----------------------------------------
    // Entreprise propriétaire (obligatoire)
    // -----------------------------------------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "entreprise_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_client_entreprise")
    )
    private Entreprise entreprise;

    // -----------------------------------------
    // User propriétaire de la boutique (obligatoire)
    // -----------------------------------------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_client_user")
    )
    private User user;

    @CreatedDate
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @LastModifiedDate
    @Column(name = "date_modification")
    private LocalDateTime dateModification;
}