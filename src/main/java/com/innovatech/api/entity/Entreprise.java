package com.innovatech.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entreprise (tenant).
 * <p>
 * Une entreprise appartient à UN ADMIN (owner) et contient :
 * des USERs (managers), des catégories, produits, clients, etc.
 */
@Entity
@Table(
        name = "entreprises",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_entreprise_slug", columnNames = "slug")
        },
        indexes = {
                @Index(name = "idx_entreprise_nom", columnList = "nom"),
                @Index(name = "idx_entreprise_owner", columnList = "owner_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "owner")
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
    private BigDecimal tvaPercent = new BigDecimal("18.00");

    @Column(name = "logo", length = 255)
    private String logo;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private boolean actif = true;

    /**
     * ADMIN propriétaire de l'entreprise.
     * Plusieurs entreprises peuvent avoir le même owner (cas rare mais possible).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "owner_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_entreprise_owner")
    )
    private User owner;

    @CreatedDate
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @LastModifiedDate
    @Column(name = "date_modification")
    private LocalDateTime dateModification;
}