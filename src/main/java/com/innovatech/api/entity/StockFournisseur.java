package com.innovatech.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Stock d'un produit chez un fournisseur spécifique.
 * <p>
 * Un même produit peut avoir plusieurs prix selon le fournisseur.
 */
@Entity
@Table(
        name = "stocks_fournisseur",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_stock_produit_fournisseur",
                        columnNames = {"produit_id", "fournisseur_id"}
                )
        },
        indexes = {
                @Index(name = "idx_stock_produit", columnList = "produit_id"),
                @Index(name = "idx_stock_fournisseur", columnList = "fournisseur_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"produit", "fournisseur"})
public class StockFournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "produit_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_stock_produit")
    )
    private Produit produit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "fournisseur_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_stock_fournisseur")
    )
    private Fournisseur fournisseur;

    /**
     * Quantité en stock chez ce fournisseur.
     */
    @Column(name = "stock", nullable = false)
    @Builder.Default
    private Integer stock = 0;

    /**
     * Prix d'achat actuel chez ce fournisseur.
     * Mis à jour à chaque commande d'achat reçue.
     */
    @Column(name = "prix_achat", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal prixAchat = BigDecimal.ZERO;

    @LastModifiedDate
    @Column(name = "date_modification")
    private LocalDateTime dateModification;
}