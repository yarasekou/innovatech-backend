package com.innovatech.api.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoProduitDTO {

    private Long id;
    private Long produitId;
    private String url;           // URL complète pour le client
    private String nomFichier;
    private String titre;
    private boolean principale;
    private Integer ordre;
    private Long taille;
    private String typeMime;
    private LocalDateTime dateCreation;
}