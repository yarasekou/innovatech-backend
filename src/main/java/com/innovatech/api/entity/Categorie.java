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
 * Catégorie hiérarchique d'un produit.
 * <p>
 * Structure d'arbre : une catégorie peut avoir un parent et plusieurs enfants.
 * Exemple : Accessoire > Téléphone > Google Pixel > 10 Pro > 128G
 */
@Entity
@Table(
        name = "categories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_categorie_slug", columnNames = "slug")
        },
        indexes = {
                @Index(name = "idx_categorie_parent", columnList = "parent_id"),
                @Index(name = "idx_categorie_niveau", columnList = "niveau"),
                @Index(name = "idx_categorie_nom", columnList = "nom")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"parent", "enfants"})
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nom d'affichage (ex: "Google Pixel").
     */
    @Column(name = "nom", nullable = false, length = 150)
    private String nom;

    /**
     * Slug unique (ex: "google-pixel"). Utilisé pour les URLs.
     */
    @Column(name = "slug", nullable = false, length = 150)
    private String slug;

    @Column(name = "description", length = 500)
    private String description;

    /**
     * Catégorie parente (null si catégorie racine).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "parent_id",
            foreignKey = @ForeignKey(name = "fk_categorie_parent")
    )
    private Categorie parent;

    /**
     * Sous-catégories directes.
     */
    @OneToMany(
            mappedBy = "parent",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<Categorie> enfants = new ArrayList<>();

    /**
     * Niveau dans la hiérarchie (1 = racine, 2 = enfant, ...).
     * Calculé automatiquement au moment de la création.
     */
    @Column(name = "niveau", nullable = false)
    @Builder.Default
    private Integer niveau = 1;

    /**
     * Chemin complet dénormalisé (ex: "Accessoire > Téléphone > Google Pixel").
     * Utile pour l'affichage rapide et les recherches.
     */
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