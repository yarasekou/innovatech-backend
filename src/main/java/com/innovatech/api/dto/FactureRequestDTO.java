package com.innovatech.api.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FactureRequestDTO {
    private Long commandeId;
    private Long commandeAchatId;

    private LocalDate dateEmission;

    @Size(max = 1000, message = "Les notes ne doivent pas dépasser 1000 caractères")
    private String notes;
}