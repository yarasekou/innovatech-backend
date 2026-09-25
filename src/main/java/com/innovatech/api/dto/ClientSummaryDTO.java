package com.innovatech.api.dto;

import lombok.*;

/**
 * Vue allégée d'un client, utilisée dans les DTOs imbriqués.
 * <p>
 * On expose seulement les infos utiles (pas les dates, adresse…).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientSummaryDTO {

    private Long id;
    private String nom;
    private String prenom;
    private String email;
}