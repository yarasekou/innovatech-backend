package com.innovatech.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Photo associée à un produit.
 */
@Entity
@Table(
        name = "photos_produit",
        indexes = {
                @Index(name = "idx_photo_produit", columnList = "produit_id"),
                @Index(name = "idx_photo_principale", columnList = "produit_id, principale")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "produit")
public class PhotoProduit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "produit_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_photo_produit")
    )
    private Produit produit;

    @Column(name = "nom_fichier", nullable = false, length = 255)
    private String nomFichier;

    @Column(name = "chemin", nullable = false, length = 500)
    private String chemin;

    @Column(name = "type_mime", length = 100)
    private String typeMime;

    @Column(name = "taille")
    private Long taille;

    @Column(name = "titre", length = 255)
    private String titre;

    @Column(name = "principale", nullable = false)
    @Builder.Default
    private boolean principale = false;

    @Column(name = "ordre")
    @Builder.Default
    private Integer ordre = 0;

    @CreatedDate
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}