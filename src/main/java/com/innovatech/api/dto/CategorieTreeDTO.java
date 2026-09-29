package com.innovatech.api.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Vue en arbre d'une catégorie (avec ses enfants récursivement).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorieTreeDTO {

    private Long id;
    private String nom;
    private String slug;
    private Integer niveau;
    private String cheminComplet;
    private boolean actif;

    @Builder.Default
    private List<CategorieTreeDTO> enfants = new ArrayList<>();
}