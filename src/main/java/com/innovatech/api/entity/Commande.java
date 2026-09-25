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
 * Entité représentant une commande passée par un client.
 */
@Entity
@Table(
        name = "commandes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_commande_numero", columnNames = "numero")
        },
        indexes = {
                @Index(name = "idx_commande_client", columnList = "client_id"),
                @Index(name = "idx_commande_statut", columnList = "statut"),
                @Index(name = "idx_commande_date", columnList = "date_commande")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"client", "lignes"})
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, length = 30)
    private String numero;

    /**
     * Client propriétaire de la commande.
     * <p>
     * LAZY : on ne charge pas le client systématiquement.
     * On le chargera explicitement quand nécessaire (via EntityGraph).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "client_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_commande_client")
    )
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    @Builder.Default
    private StatutCommande statut = StatutCommande.EN_ATTENTE;

    @Column(name = "total_ht", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalHT = BigDecimal.ZERO;

    @Column(name = "tva_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal tvaPercent = new BigDecimal("18.00");

    @Column(name = "total_ttc", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalTTC = BigDecimal.ZERO;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "date_commande", nullable = false)
    @Builder.Default
    private LocalDateTime dateCommande = LocalDateTime.now();

    @Column(name = "date_livraison_prevue")
    private LocalDate dateLivraisonPrevue;

    @CreatedDate
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @LastModifiedDate
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @OneToMany(
            mappedBy = "commande",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<LigneCommande> lignes = new ArrayList<>();

    /**
     * Ajoute une ligne et lie la relation bidirectionnelle.
     */
    public void ajouterLigne(LigneCommande ligne) {
        lignes.add(ligne);
        ligne.setCommande(this);
    }

    /**
     * Retire une ligne (orphanRemoval supprimera de la BDD).
     */
    public void retirerLigne(LigneCommande ligne) {
        lignes.remove(ligne);
        ligne.setCommande(null);
    }

    /**
     * Vide toutes les lignes.
     */
    public void viderLignes() {
        lignes.forEach(l -> l.setCommande(null));
        lignes.clear();
    }
}