package com.innovatech.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignPatronDTO {

    @NotNull(message = "L'identifiant du patron est obligatoire")
    private Long patronId;
}