package com.innovatech.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entreprise appartenant à un USER (relation 1-1).
 * <p>
 * C'est le conteneur principal des données métier :
 * catégories, produits, clients, fournisseurs, commandes, etc.
 */
@Entity
@Table(
        name = "entreprises",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_entreprise_slug", columnNames = "slug"),
                @UniqueConstraint(name = "uk_entreprise_user", columnNames = "user_id")
        },
        indexes = {
                @Index(name = "idx_entreprise_nom", columnList = "nom"),
                @Index(name = "idx_entreprise_user", columnList = "user_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
public class Entreprise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, length = 150)
    private String nom;

    @Column(name = "slug", nullable = false, length = 150)
    private String slug;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "telephone", length = 20)
    private String telephone;

    @Column(name = "adresse", length = 255)
    private String adresse;

    @Column(name = "ville", length = 100)
    private String ville;

    @Column(name = "pays", length = 100)
    @Builder.Default
    private String pays = "Sénégal";

    @Column(name = "siret", length = 14)
    private String siret;

    @Column(name = "devise", nullable = false, length = 10)
    @Builder.Default
    private String devise = "XOF";

    @Column(name = "tva_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private java.math.BigDecimal tvaPercent = new java.math.BigDecimal("18.00");

    /**
     * Nom du fichier du logo (dans /uploads/entreprises/).
     */
    @Column(name = "logo", length = 255)
    private String logo;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private boolean actif = true;

    /**
     * Propriétaire (USER). Relation 1-1 : un USER a une seule entreprise.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_entreprise_user")
    )
    private User user;

    @CreatedDate
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @LastModifiedDate
    @Column(name = "date_modification")
    private LocalDateTime dateModification;
}