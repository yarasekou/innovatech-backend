package com.innovatech.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_user_username", columnNames = "username")
        },
        indexes = {
                @Index(name = "idx_user_patron", columnList = "patron_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"password", "patron", "employes", "entreprise"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "prenom", length = 100)
    private String prenom;

    @Column(name = "nom", length = 100)
    private String nom;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 50)
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // -----------------------------------------
    // Relation hiérarchique : SELLER → USER (patron)
    // -----------------------------------------

    /**
     * Le patron de cet utilisateur (null pour ADMIN et USER, rempli pour SELLER).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "patron_id",
            foreignKey = @ForeignKey(name = "fk_user_patron")
    )
    private User patron;

    /**
     * Les employés (SELLERs) rattachés à cet utilisateur.
     */
    @OneToMany(
            mappedBy = "patron",
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private Set<User> employes = new HashSet<>();

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
    // Entreprise (1-1)
    // -----------------------------------------

    /**
     * L'entreprise du USER (null tant qu'elle n'est pas créée).
     */
    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Entreprise entreprise;

    // -----------------------------------------
    // Méthodes utilitaires
    // -----------------------------------------

    public void ajouterEmploye(User employe) {
        employes.add(employe);
        employe.setPatron(this);
    }

    public void retirerEmploye(User employe) {
        employes.remove(employe);
        employe.setPatron(null);
    }
}