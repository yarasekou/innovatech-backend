package com.innovatech.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Ligne d'un retour : un produit + une quantité retournée.
 */
@Entity
@Table(
        name = "lignes_retour",
        indexes = {
                @Index(name = "idx_ligne_retour_retour", columnList = "retour_id"),
                @Index(name = "idx_ligne_retour_produit", columnList = "produit_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"retour", "produit"})
public class LigneRetour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "retour_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ligne_retour_retour")
    )
    private Retour retour;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "produit_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ligne_retour_produit")
    )
    private Produit produit;

    @Column(name = "quantite", nullable = false)
    private Integer quantite;

    /**
     * Prix unitaire au moment de la commande d'origine (figé).
     */
    @Column(name = "prix_unitaire", nullable = false, precision = 12, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(name = "sous_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal sousTotal;

    @Column(name = "raison", length = 500)
    private String raison;

    public void calculerSousTotal() {
        if (quantite != null && prixUnitaire != null) {
            this.sousTotal = prixUnitaire.multiply(BigDecimal.valueOf(quantite));
        } else {
            this.sousTotal = BigDecimal.ZERO;
        }
    }
}