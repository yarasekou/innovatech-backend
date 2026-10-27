package com.innovatech.api.mapper;

import com.innovatech.api.dto.FactureResponseDTO;
import com.innovatech.api.entity.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class FactureMapper {

    // Pour une commande client
    public Facture toEntity(Commande commande, Entreprise entreprise, User user, String numero, LocalDate dateEmission, String notes) {
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
                .clientNom(client.getPrenom() + " " + client.getNom())
                .clientAdresse(buildAdresse(client))
                .clientSiret(client.getSiret())
                .clientEmail(client.getEmail())
                .clientTelephone(client.getTelephone())
                .totalHT(commande.getTotalHT())
                .tvaPercent(commande.getTvaPercent())
                .totalTTC(commande.getTotalTTC())
                .dateEmission(emission)
                .dateEcheance(echeance)
                .statut(StatutFacture.BROUILLON)
                .notes(notes)
                .build();
    }

    // ⚠️ NOUVEAU : Pour une commande d'achat
    public Facture toEntityAchat(CommandeAchat commandeAchat, Entreprise entreprise, User user, String numero, LocalDate dateEmission, String notes) {
        if (commandeAchat == null) return null;

        Fournisseur fournisseur = commandeAchat.getFournisseur();
        LocalDate emission = dateEmission != null ? dateEmission : LocalDate.now();
        LocalDate echeance = emission.plusDays(30);

        return Facture.builder()
                .numero(numero)
                .entreprise(entreprise)
                .user(user)
                .commandeAchat(commandeAchat)
                .clientNom(fournisseur.getNom()) // On utilise les champs client pour stocker le fournisseur
                .clientAdresse(fournisseur.getAdresse())
                .clientEmail(fournisseur.getEmail())
                .clientTelephone(fournisseur.getTelephone())
                .totalHT(commandeAchat.getTotalHT())
                .tvaPercent(commandeAchat.getTvaPercent())
                .totalTTC(commandeAchat.getTotalTTC())
                .dateEmission(emission)
                .dateEcheance(echeance)
                .statut(StatutFacture.BROUILLON)
                .notes(notes)
                .build();
    }

    public FactureResponseDTO toResponseDTO(Facture entity) {
        if (entity == null) return null;

        FactureResponseDTO.FactureResponseDTOBuilder builder = FactureResponseDTO.builder()
                .id(entity.getId())
                .numero(entity.getNumero())
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

        if (entity.getClient() != null) {
            builder.clientId(entity.getClient().getId());
        }

        if (entity.getCommande() != null) {
            builder.commandeId(entity.getCommande().getId())
                    .commandeNumero(entity.getCommande().getNumero());
        }

        // ⚠️ NOUVEAU
        if (entity.getCommandeAchat() != null) {
            builder.commandeAchatId(entity.getCommandeAchat().getId())
                    .commandeAchatNumero(entity.getCommandeAchat().getNumero());
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