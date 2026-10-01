package com.innovatech.api.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;

/**
 * Top client (par CA).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopClientDTO {

    private Long clientId;
    private String nom;
    private String prenom;
    private String email;
    private Long nbCommandes;
    private BigDecimal chiffreAffaires;
}