package com.innovatech.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "lignes_commande_achat",
        indexes = {
                @Index(name = "idx_ligne_achat_commande", columnList = "commande_achat_id"),
                @Index(name = "idx_ligne_achat_produit", columnList = "produit_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"commandeAchat", "produit"})
public class LigneCommandeAchat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "commande_achat_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ligne_achat_commande")
    )
    private CommandeAchat commandeAchat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "produit_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ligne_achat_produit")
    )
    private Produit produit;

    @Column(name = "quantite", nullable = false)
    private Integer quantite;

    /**
     * Prix d'achat unitaire (figé au moment de la commande).
     * Peut différer du prix d'achat du produit (négociation).
     */
    @Column(name = "prix_achat_unitaire", nullable = false, precision = 12, scale = 2)
    private BigDecimal prixAchatUnitaire;

    @Column(name = "sous_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal sousTotal;

    public void calculerSousTotal() {
        if (quantite != null && prixAchatUnitaire != null) {
            this.sousTotal = prixAchatUnitaire.multiply(BigDecimal.valueOf(quantite));
        } else {
            this.sousTotal = BigDecimal.ZERO;
        }
    }
}