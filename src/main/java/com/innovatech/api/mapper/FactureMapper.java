package com.innovatech.api.mapper;

import com.innovatech.api.dto.FactureResponseDTO;
import com.innovatech.api.entity.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class FactureMapper {

    /**
     * Crée une facture à partir d'une commande, en figeant les données.
     */
    public Facture toEntity(Commande commande,
                            Entreprise entreprise,
                            User user,
                            String numero,
                            LocalDate dateEmission,
                            String notes) {
        if (commande == null) return null;

        Client client = commande.getClient();
        LocalDate emission = dateEmission != null ? dateEmission : LocalDate.now();
        LocalDate echeance = emission.plusDays(30);

        return Facture.builder()
                .numero(numero)
                .entreprise(entreprise)
                .user(user)
                .commande(commande)
                .client(client)
                // Snapshot client
                .clientNom(client.getPrenom() + " " + client.getNom())
                .clientAdresse(buildAdresse(client))
                .clientSiret(client.getSiret())
                .clientEmail(client.getEmail())
                .clientTelephone(client.getTelephone())
                // Snapshot montants
                .totalHT(commande.getTotalHT())
                .tvaPercent(commande.getTvaPercent())
                .totalTTC(commande.getTotalTTC())
                // Dates
                .dateEmission(emission)
                .dateEcheance(echeance)
                // Statut
                .statut(StatutFacture.BROUILLON)
                .notes(notes)
                .build();
    }

    public FactureResponseDTO toResponseDTO(Facture entity) {
        if (entity == null) return null;

        FactureResponseDTO.FactureResponseDTOBuilder builder = FactureResponseDTO.builder()
                .id(entity.getId())
                .numero(entity.getNumero())
                .clientId(entity.getClient() != null ? entity.getClient().getId() : null)
                .clientNom(entity.getClientNom())
                .clientAdresse(entity.getClientAdresse())
                .clientSiret(entity.getClientSiret())
                .clientEmail(entity.getClientEmail())
                .clientTelephone(entity.getClientTelephone())
                .totalHT(entity.getTotalHT())
                .tvaPercent(entity.getTvaPercent())
                .totalTTC(entity.getTotalTTC())
                .dateEmission(entity.getDateEmission())
                .dateEcheance(entity.getDateEcheance())
                .datePaiement(entity.getDatePaiement())
                .statut(entity.getStatut())
                .statutLibelle(entity.getStatut() != null ? entity.getStatut().getLibelle() : null)
                .enRetard(entity.estEnRetard())
                .joursDeRetard(entity.joursDeRetard())
                .pdfUrl(entity.getPdfUrl())
                .notes(entity.getNotes())
                .dateCreation(entity.getDateCreation())
                .dateModification(entity.getDateModification());

        if (entity.getCommande() != null) {
            builder.commandeId(entity.getCommande().getId())
                    .commandeNumero(entity.getCommande().getNumero());
        }

        if (entity.getEntreprise() != null) {
            builder.entrepriseId(entity.getEntreprise().getId())
                    .entrepriseNom(entity.getEntreprise().getNom());
        }

        if (entity.getUser() != null) {
            builder.userId(entity.getUser().getId())
                    .userEmail(entity.getUser().getEmail())
                    .userNom(entity.getUser().getNom())
                    .userPrenom(entity.getUser().getPrenom());
        }

        return builder.build();
    }

    private String buildAdresse(Client client) {
        StringBuilder sb = new StringBuilder();
        if (client.getAdresse() != null) sb.append(client.getAdresse());
        if (client.getVille() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(client.getVille());
        }
        if (client.getPays() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(client.getPays());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}