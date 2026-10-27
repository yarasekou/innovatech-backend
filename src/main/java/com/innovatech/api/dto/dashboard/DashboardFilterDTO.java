package com.innovatech.api.dto.dashboard;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardFilterDTO {
    private LocalDate dateDebut;
    private LocalDate dateFin;
}