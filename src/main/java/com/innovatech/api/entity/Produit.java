package com.innovatech.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "produits",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_produit_reference", columnNames = "reference")
        },
        indexes = {
                @Index(name = "idx_produit_nom", columnList = "nom"),
                @Index(name = "idx_produit_categorie", columnList = "categorie")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference", nullable = false, length = 50)
    private String reference;

    @Column(name = "nom", nullable = false, length = 150)
    private String nom;

    @Column(name = "description", length = 1000)
    private String description;

    /**
     * Prix de VENTE au client (HT).
     */
    @Column(name = "prix", nullable = false, precision = 12, scale = 2)
    private BigDecimal prix;

    /**
     * Prix d'ACHAT auprès du fournisseur (HT).
     * Utilisé pour calculer la marge.
     */
    @Column(name = "prix_achat", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal prixAchat = BigDecimal.ZERO;

    @Column(name = "stock", nullable = false)
    @Builder.Default
    private Integer stock = 0;

    @Column(name = "categorie", length = 100)
    private String categorie;

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
    // MÉTHODES CALCULÉES
    // -----------------------------------------

    /**
     * Marge unitaire = prix de vente - prix d'achat.
     */
    public BigDecimal getMargeUnitaire() {
        if (prix == null || prixAchat == null) {
            return BigDecimal.ZERO;
        }
        return prix.subtract(prixAchat);
    }

    /**
     * Taux de marge en pourcentage.
     * Formule : (marge / prix achat) × 100
     */
    public BigDecimal getTauxMarge() {
        if (prixAchat == null || prixAchat.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getMargeUnitaire()
                .multiply(new BigDecimal("100"))
                .divide(prixAchat, 2, RoundingMode.HALF_UP);
    }
}