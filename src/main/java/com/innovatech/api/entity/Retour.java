package com.innovatech.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Retour client d'un ou plusieurs produits d'une commande.
 * <p>
 * Scopé par entreprise ET par boutique (user).
 */
@Entity
@Table(
        name = "retours",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_retour_numero_entreprise_user",
                        columnNames = {"numero", "entreprise_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_retour_commande", columnList = "commande_id"),
                @Index(name = "idx_retour_client", columnList = "client_id"),
                @Index(name = "idx_retour_statut", columnList = "statut"),
                @Index(name = "idx_retour_entreprise", columnList = "entreprise_id"),
                @Index(name = "idx_retour_user", columnList = "user_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"commande", "client", "user", "entreprise", "lignes"})
public class Retour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, length = 30)
    private String numero;

    // -----------------------------------------
    // Relations obligatoires
    // -----------------------------------------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "entreprise_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_retour_entreprise")
    )
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_retour_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "commande_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_retour_commande")
    )
    private Commande commande;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "client_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_retour_client")
    )
    private Client client;

    // -----------------------------------------
    // Données du retour
    // -----------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    @Builder.Default
    private StatutRetour statut = StatutRetour.DEMANDE;

    @Column(name = "motif", length = 500)
    private String motif;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "montant_total", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montantTotal = BigDecimal.ZERO;

    @Column(name = "date_demande", nullable = false)
    @Builder.Default
    private LocalDateTime dateDemande = LocalDateTime.now();

    @Column(name = "date_traitement")
    private LocalDateTime dateTraitement;

    @OneToMany(
            mappedBy = "retour",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<LigneRetour> lignes = new ArrayList<>();

    @CreatedDate
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @LastModifiedDate
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    // -----------------------------------------
    // Méthodes utilitaires
    // -----------------------------------------

    public void ajouterLigne(LigneRetour ligne) {
        lignes.add(ligne);
        ligne.setRetour(this);
    }

    public void viderLignes() {
        lignes.forEach(l -> l.setRetour(null));
        lignes.clear();
    }
}