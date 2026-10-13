package com.innovatech.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Commande d'achat auprès d'un fournisseur.
 * <p>
 * Scopée par entreprise ET par boutique (user = acheteur).
 * Le numéro est unique par (entreprise_id, user_id).
 */
@Entity
@Table(
        name = "commandes_achat",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_commande_achat_numero_entreprise_user",
                        columnNames = {"numero", "entreprise_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_commande_achat_fournisseur", columnList = "fournisseur_id"),
                @Index(name = "idx_commande_achat_statut", columnList = "statut"),
                @Index(name = "idx_commande_achat_date", columnList = "date_commande"),
                @Index(name = "idx_commande_achat_entreprise", columnList = "entreprise_id"),
                @Index(name = "idx_commande_achat_user", columnList = "user_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"fournisseur", "user", "entreprise", "lignes"})
public class CommandeAchat {

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
            foreignKey = @ForeignKey(name = "fk_commande_achat_entreprise")
    )
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_commande_achat_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "fournisseur_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_commande_achat_fournisseur")
    )
    private Fournisseur fournisseur;

    // -----------------------------------------
    // Statut et montants
    // -----------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    @Builder.Default
    private StatutAchat statut = StatutAchat.EN_ATTENTE;

    @Column(name = "total_ht", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalHT = BigDecimal.ZERO;

    @Column(name = "tva_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal tvaPercent = new BigDecimal("18.00");

    @Column(name = "total_ttc", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalTTC = BigDecimal.ZERO;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "date_commande", nullable = false)
    @Builder.Default
    private LocalDateTime dateCommande = LocalDateTime.now();

    @Column(name = "date_reception_prevue")
    private LocalDate dateReceptionPrevue;

    // -----------------------------------------
    // Lignes
    // -----------------------------------------

    @OneToMany(
            mappedBy = "commandeAchat",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<LigneCommandeAchat> lignes = new ArrayList<>();

    @CreatedDate
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @LastModifiedDate
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    // -----------------------------------------
    // Méthodes utilitaires
    // -----------------------------------------

    public void ajouterLigne(LigneCommandeAchat ligne) {
        lignes.add(ligne);
        ligne.setCommandeAchat(this);
    }

    public void viderLignes() {
        lignes.forEach(l -> l.setCommandeAchat(null));
        lignes.clear();
    }
}