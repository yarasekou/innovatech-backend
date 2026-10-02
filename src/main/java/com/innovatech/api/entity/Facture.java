package com.innovatech.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Facture émise pour une commande client.
 * <p>
 * Contient un <b>snapshot</b> des informations client au moment de l'émission,
 * pour préserver l'intégrité historique en cas de modification ultérieure
 * du client (adresse, SIRET, nom…).
 */
@Entity
@Table(
        name = "factures",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_facture_numero", columnNames = "numero"),
                @UniqueConstraint(name = "uk_facture_commande", columnNames = "commande_id")
        },
        indexes = {
                @Index(name = "idx_facture_client", columnList = "client_id"),
                @Index(name = "idx_facture_statut", columnList = "statut"),
                @Index(name = "idx_facture_date_emission", columnList = "date_emission"),
                @Index(name = "idx_facture_date_echeance", columnList = "date_echeance")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"commande", "client"})
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, length = 30)
    private String numero;

    // -----------------------------------------
    // Liens
    // -----------------------------------------

    /**
     * Commande d'origine (une facture par commande maximum).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "commande_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_facture_commande")
    )
    private Commande commande;

    /**
     * Client actuel (pour la navigation depuis la facture).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "client_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_facture_client")
    )
    private Client client;

    // -----------------------------------------
    // SNAPSHOT CLIENT (figé à l'émission)
    // -----------------------------------------

    @Column(name = "client_nom", nullable = false, length = 200)
    private String clientNom;         // "Diop Aminata" (concaténé prénom + nom)

    @Column(name = "client_adresse", length = 255)
    private String clientAdresse;     // "Sacré-Cœur 3, Dakar"

    @Column(name = "client_siret", length = 14)
    private String clientSiret;

    @Column(name = "client_email", length = 150)
    private String clientEmail;

    @Column(name = "client_telephone", length = 20)
    private String clientTelephone;

    // -----------------------------------------
    // SNAPSHOT MONTANTS (figé à l'émission)
    // -----------------------------------------

    @Column(name = "total_ht", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalHT;

    @Column(name = "tva_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal tvaPercent;

    @Column(name = "total_ttc", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalTTC;

    // -----------------------------------------
    // Dates
    // -----------------------------------------

    @Column(name = "date_emission", nullable = false)
    private LocalDate dateEmission;

    /**
     * Date d'échéance = date d'émission + 30 jours.
     */
    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance;

    @Column(name = "date_paiement")
    private LocalDate datePaiement;

    // -----------------------------------------
    // Statut & PDF
    // -----------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    @Builder.Default
    private StatutFacture statut = StatutFacture.BROUILLON;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @Column(name = "notes", length = 1000)
    private String notes;

    // -----------------------------------------
    // Audit
    // -----------------------------------------

    @CreatedDate
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @LastModifiedDate
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    // -----------------------------------------
    // Méthodes utilitaires
    // -----------------------------------------

    /**
     * Vérifie si la facture est en retard de paiement.
     */
    public boolean estEnRetard() {
        if (statut == StatutFacture.PAYEE || statut == StatutFacture.ANNULEE) {
            return false;
        }
        return LocalDate.now().isAfter(dateEcheance);
    }

    /**
     * Calcule le nombre de jours de retard (0 si non en retard).
     */
    public long joursDeRetard() {
        if (!estEnRetard()) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(dateEcheance, LocalDate.now());
    }
}