package com.innovatech.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Ligne d'une commande : un produit + une quantité.
 * <p>
 * Le prix unitaire est copié au moment de la commande (historisation).
 */
@Entity
@Table(
        name = "lignes_commande",
        indexes = {
                @Index(name = "idx_ligne_commande", columnList = "commande_id"),
                @Index(name = "idx_ligne_produit", columnList = "produit_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"commande", "produit"})
public class LigneCommande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "commande_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ligne_commande")
    )
    private Commande commande;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "produit_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ligne_produit")
    )
    private Produit produit;

    @Column(name = "quantite", nullable = false)
    private Integer quantite;

    /**
     * Prix unitaire HT au moment de la commande.
     * Historisation : si le produit change de prix, la ligne reste correcte.
     */
    @Column(name = "prix_unitaire", nullable = false, precision = 12, scale = 2)
    private BigDecimal prixUnitaire;

    /**
     * Sous-total = quantite × prixUnitaire.
     * Calculé et stocké pour ne pas recalculer à chaque lecture.
     */
    @Column(name = "sous_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal sousTotal;

    /**
     * Calcule le sous-total à partir de la quantité et du prix unitaire.
     */
    public void calculerSousTotal() {
        if (quantite != null && prixUnitaire != null) {
            this.sousTotal = prixUnitaire.multiply(BigDecimal.valueOf(quantite));
        } else {
            this.sousTotal = BigDecimal.ZERO;
        }
    }
}