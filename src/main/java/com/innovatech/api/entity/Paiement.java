package com.innovatech.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Paiement reçu pour une commande.
 * <p>
 * Scopé par entreprise ET par boutique (user).
 * Le numéro est unique par (entreprise_id, user_id).
 */
@Entity
@Table(
        name = "paiements",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_paiement_numero_entreprise_user",
                        columnNames = {"numero", "entreprise_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_paiement_commande", columnList = "commande_id"),
                @Index(name = "idx_paiement_statut", columnList = "statut"),
                @Index(name = "idx_paiement_date", columnList = "date_paiement"),
                @Index(name = "idx_paiement_entreprise", columnList = "entreprise_id"),
                @Index(name = "idx_paiement_user", columnList = "user_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"commande", "user", "entreprise"})
public class Paiement {

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
            foreignKey = @ForeignKey(name = "fk_paiement_entreprise")
    )
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_paiement_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "commande_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_paiement_commande")
    )
    private Commande commande;

    // -----------------------------------------
    // Données du paiement
    // -----------------------------------------

    @Column(name = "montant", nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(name = "methode", nullable = false, length = 30)
    private MethodePaiement methode;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    @Builder.Default
    private StatutPaiement statut = StatutPaiement.EN_ATTENTE;

    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "date_paiement", nullable = false)
    @Builder.Default
    private LocalDateTime datePaiement = LocalDateTime.now();

    @CreatedDate
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @LastModifiedDate
    @Column(name = "date_modification")
    private LocalDateTime dateModification;
}