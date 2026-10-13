package com.innovatech.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Catégorie hiérarchique d'un produit, scopée par entreprise ET par boutique (user).
 * <p>
 * Structure d'arbre : une catégorie peut avoir un parent et plusieurs enfants.
 * <p>
 * Un même slug peut exister dans plusieurs boutiques de la même entreprise.
 */
@Entity
@Table(
        name = "categories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_categorie_slug_entreprise_user",
                        columnNames = {"slug", "entreprise_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_categorie_parent", columnList = "parent_id"),
                @Index(name = "idx_categorie_niveau", columnList = "niveau"),
                @Index(name = "idx_categorie_nom", columnList = "nom"),
                @Index(name = "idx_categorie_entreprise", columnList = "entreprise_id"),
                @Index(name = "idx_categorie_user", columnList = "user_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"parent", "enfants", "entreprise", "user"})
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, length = 150)
    private String nom;

    @Column(name = "slug", nullable = false, length = 150)
    private String slug;

    @Column(name = "description", length = 500)
    private String description;

    // -----------------------------------------
    // Entreprise propriétaire (obligatoire)
    // -----------------------------------------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "entreprise_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_categorie_entreprise")
    )
    private Entreprise entreprise;

    // -----------------------------------------
    // User propriétaire de la boutique (obligatoire)
    // -----------------------------------------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_categorie_user")
    )
    private User user;

    // -----------------------------------------
    // Hiérarchie
    // -----------------------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "parent_id",
            foreignKey = @ForeignKey(name = "fk_categorie_parent")
    )
    private Categorie parent;

    @OneToMany(
            mappedBy = "parent",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<Categorie> enfants = new ArrayList<>();

    @Column(name = "niveau", nullable = false)
    @Builder.Default
    private Integer niveau = 1;

    @Column(name = "chemin_complet", length = 1000)
    private String cheminComplet;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private boolean actif = true;

    @CreatedDate
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @LastModifiedDate
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    // -----------------------------------------
    // Méthodes utilitaires
    // -----------------------------------------

    public void ajouterEnfant(Categorie enfant) {
        enfants.add(enfant);
        enfant.setParent(this);
    }

    public void retirerEnfant(Categorie enfant) {
        enfants.remove(enfant);
        enfant.setParent(null);
    }
}