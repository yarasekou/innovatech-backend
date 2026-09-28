package com.innovatech.api.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FournisseurSummaryDTO {

    private Long id;
    private String code;
    private String nom;
    private String email;
}