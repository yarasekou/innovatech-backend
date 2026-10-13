package com.innovatech.api.dto;

import lombok.*;

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