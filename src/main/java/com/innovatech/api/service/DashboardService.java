package com.innovatech.api.service;

import com.innovatech.api.dto.dashboard.*;
import com.innovatech.api.entity.Entreprise;
import com.innovatech.api.entity.Role;
import com.innovatech.api.entity.StatutCommande;
import com.innovatech.api.entity.User;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.repository.*;
import com.innovatech.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private final CommandeRepository commandeRepository;
    private final ClientRepository clientRepository;
    private final ProduitRepository produitRepository;
    private final PaiementRepository paiementRepository;
    private final CommandeAchatRepository commandeAchatRepository;
    private final UserRepository userRepository;
    private final EntrepriseRepository entrepriseRepository; // ⚠️ AJOUTÉ ICI

    private static final String[] MOIS_NOMS = {
            "", "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    };

    // -----------------------------------------
    // STATS GLOBALES
    // -----------------------------------------

    public DashboardStatsDTO getStats(LocalDate dateDebut, LocalDate dateFin) {
        User user = getCurrentUser();

        if (user.hasRole(Role.ROLE_SUPER_ADMIN)) {
            return getStatsForSuperAdmin();
        }

        if (user.getEntreprise() == null) {
            throw new BusinessException("Aucune entreprise associée.");
        }

        Long entrepriseId = user.getEntreprise().getId();
        LocalDateTime start = dateDebut != null ? dateDebut.atStartOfDay() : null;
        LocalDateTime end = dateFin != null ? dateFin.atTime(23, 59, 59) : null;

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return getStatsForEntreprise(entrepriseId, start, end);
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return getStatsForBoutique(entrepriseId, shopOwnerId, start, end);
    }

    private DashboardStatsDTO getStatsForSuperAdmin() {
        return DashboardStatsDTO.builder()
                .chiffreAffairesHT(BigDecimal.ZERO)
                .chiffreAffairesTTC(BigDecimal.ZERO)
                .margeTotale(BigDecimal.ZERO)
                .nbClients(clientRepository.count())
                .nbProduits(produitRepository.count())
                .nbCommandes(commandeRepository.count())
                .nbPaiements(paiementRepository.count())
                .nbCommandesAchat(commandeAchatRepository.count())
                .nbCommandesEnAttente(commandeRepository.countByEntrepriseIdAndStatut(null, StatutCommande.EN_ATTENTE))
                .nbCommandesValidees(commandeRepository.countByEntrepriseIdAndStatut(null, StatutCommande.VALIDEE))
                .nbCommandesEnCours(commandeRepository.countByEntrepriseIdAndStatut(null, StatutCommande.EN_COURS))
                .nbCommandesLivrees(commandeRepository.countByEntrepriseIdAndStatut(null, StatutCommande.LIVREE))
                .nbCommandesAnnulees(commandeRepository.countByEntrepriseIdAndStatut(null, StatutCommande.ANNULEE))
                .panierMoyen(BigDecimal.ZERO)
                .build();
    }

    private DashboardStatsDTO getStatsForEntreprise(Long entrepriseId, LocalDateTime start, LocalDateTime end) {
        BigDecimal caTTC = commandeRepository.sumTotalTTCByEntreprise(entrepriseId, start, end);
        BigDecimal caHT = commandeRepository.sumTotalHTByEntreprise(entrepriseId, start, end);
        BigDecimal marge = commandeRepository.sumMargeByEntreprise(entrepriseId, start, end);

        long nbCommandes = commandeRepository.countByEntrepriseId(entrepriseId);
        BigDecimal panierMoyen = nbCommandes > 0
                ? caTTC.divide(BigDecimal.valueOf(nbCommandes), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return DashboardStatsDTO.builder()
                .chiffreAffairesHT(caHT)
                .chiffreAffairesTTC(caTTC)
                .margeTotale(marge)
                .nbClients(clientRepository.countByEntrepriseId(entrepriseId))
                .nbProduits(produitRepository.countByEntrepriseId(entrepriseId))
                .nbCommandes(nbCommandes)
                .nbPaiements(0L)
                .nbCommandesAchat(commandeAchatRepository.countByEntrepriseId(entrepriseId))
                .nbCommandesEnAttente(commandeRepository.countByEntrepriseIdAndStatutDate(entrepriseId, StatutCommande.EN_ATTENTE, start, end))
                .nbCommandesValidees(commandeRepository.countByEntrepriseIdAndStatutDate(entrepriseId, StatutCommande.VALIDEE, start, end))
                .nbCommandesEnCours(commandeRepository.countByEntrepriseIdAndStatutDate(entrepriseId, StatutCommande.EN_COURS, start, end))
                .nbCommandesLivrees(commandeRepository.countByEntrepriseIdAndStatutDate(entrepriseId, StatutCommande.LIVREE, start, end))
                .nbCommandesAnnulees(commandeRepository.countByEntrepriseIdAndStatutDate(entrepriseId, StatutCommande.ANNULEE, start, end))
                .panierMoyen(panierMoyen)
                .build();
    }

    private DashboardStatsDTO getStatsForBoutique(Long entrepriseId, Long shopOwnerId, LocalDateTime start, LocalDateTime end) {
        BigDecimal caTTC = commandeRepository.sumTotalTTCByBoutique(entrepriseId, shopOwnerId, start, end);
        BigDecimal caHT = commandeRepository.sumTotalHTByBoutique(entrepriseId, shopOwnerId, start, end);
        BigDecimal marge = commandeRepository.sumMargeByBoutique(entrepriseId, shopOwnerId, start, end);

        long nbCommandes = commandeRepository.countByEntrepriseIdAndUserId(entrepriseId, shopOwnerId);
        BigDecimal panierMoyen = nbCommandes > 0
                ? caTTC.divide(BigDecimal.valueOf(nbCommandes), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return DashboardStatsDTO.builder()
                .chiffreAffairesHT(caHT)
                .chiffreAffairesTTC(caTTC)
                .margeTotale(marge)
                .nbClients(clientRepository.countByEntrepriseIdAndUserId(entrepriseId, shopOwnerId))
                .nbProduits(produitRepository.countByEntrepriseIdAndUserId(entrepriseId, shopOwnerId))
                .nbCommandes(nbCommandes)
                .nbPaiements(0L)
                .nbCommandesAchat(commandeAchatRepository.countByEntrepriseIdAndUserId(entrepriseId, shopOwnerId))
                .nbCommandesEnAttente(commandeRepository.countByEntrepriseIdAndUserIdAndStatutDate(entrepriseId, shopOwnerId, StatutCommande.EN_ATTENTE, start, end))
                .nbCommandesValidees(commandeRepository.countByEntrepriseIdAndUserIdAndStatutDate(entrepriseId, shopOwnerId, StatutCommande.VALIDEE, start, end))
                .nbCommandesEnCours(commandeRepository.countByEntrepriseIdAndUserIdAndStatutDate(entrepriseId, shopOwnerId, StatutCommande.EN_COURS, start, end))
                .nbCommandesLivrees(commandeRepository.countByEntrepriseIdAndUserIdAndStatutDate(entrepriseId, shopOwnerId, StatutCommande.LIVREE, start, end))
                .nbCommandesAnnulees(commandeRepository.countByEntrepriseIdAndUserIdAndStatutDate(entrepriseId, shopOwnerId, StatutCommande.ANNULEE, start, end))
                .panierMoyen(panierMoyen)
                .build();
    }

    // -----------------------------------------
    // TOP PRODUITS & CLIENTS
    // -----------------------------------------

    public List<TopProduitDTO> getTopProduits(int limit, LocalDate dateDebut, LocalDate dateFin) {
        User user = getCurrentUser();
        if (user.hasRole(Role.ROLE_SUPER_ADMIN)) return List.of();

        Long entrepriseId = user.getEntreprise().getId();
        Long shopOwnerId = user.hasRole(Role.ROLE_ADMIN) ? null : resolveShopOwnerId(user);
        LocalDateTime start = dateDebut != null ? dateDebut.atStartOfDay() : null;
        LocalDateTime end = dateFin != null ? dateFin.atTime(23, 59, 59) : null;

        return commandeRepository.findTopProduits(entrepriseId, shopOwnerId, start, end, PageRequest.of(0, limit))
                .stream()
                .map(obj -> TopProduitDTO.builder()
                        .produitId((Long) obj[0])
                        .reference((String) obj[1])
                        .nom((String) obj[2])
                        .quantiteVendue((Long) obj[3])
                        .chiffreAffaires((BigDecimal) obj[4])
                        .build())
                .toList();
    }

    public List<TopClientDTO> getTopClients(int limit, LocalDate dateDebut, LocalDate dateFin) {
        User user = getCurrentUser();
        if (user.hasRole(Role.ROLE_SUPER_ADMIN)) return List.of();

        Long entrepriseId = user.getEntreprise().getId();
        Long shopOwnerId = user.hasRole(Role.ROLE_ADMIN) ? null : resolveShopOwnerId(user);
        LocalDateTime start = dateDebut != null ? dateDebut.atStartOfDay() : null;
        LocalDateTime end = dateFin != null ? dateFin.atTime(23, 59, 59) : null;

        return commandeRepository.findTopClients(entrepriseId, shopOwnerId, start, end, PageRequest.of(0, limit))
                .stream()
                .map(obj -> TopClientDTO.builder()
                        .clientId((Long) obj[0])
                        .nom((String) obj[1])
                        .prenom((String) obj[2])
                        .email((String) obj[3])
                        .nbCommandes((Long) obj[4])
                        .chiffreAffaires((BigDecimal) obj[5])
                        .build())
                .toList();
    }

    public List<ChiffreAffairesMoisDTO> getChiffreAffairesMensuel(LocalDate dateDebut, LocalDate dateFin) {
        User user = getCurrentUser();
        if (user.hasRole(Role.ROLE_SUPER_ADMIN)) return List.of();

        Long entrepriseId = user.getEntreprise().getId();
        Long shopOwnerId = user.hasRole(Role.ROLE_ADMIN) ? null : resolveShopOwnerId(user);
        LocalDateTime start = dateDebut != null ? dateDebut.atStartOfDay() : null;
        LocalDateTime end = dateFin != null ? dateFin.atTime(23, 59, 59) : null;

        return commandeRepository.findChiffreAffairesMensuel(entrepriseId, shopOwnerId, start, end)
                .stream()
                .map(obj -> {
                    int annee = ((Number) obj[0]).intValue();
                    int mois = ((Number) obj[1]).intValue();
                    return ChiffreAffairesMoisDTO.builder()
                            .annee(annee)
                            .mois(mois)
                            .libelle(MOIS_NOMS[mois] + " " + annee)
                            .chiffreAffaires((BigDecimal) obj[2])
                            .nbCommandes(((Number) obj[3]).longValue())
                            .build();
                })
                .toList();
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    private Long resolveShopOwnerId(User user) {
        if (user.hasRole(Role.ROLE_SELLER)) {
            if (user.getPatron() == null) {
                throw new BusinessException("Ce SELLER n'a pas de patron assigné");
            }
            return user.getPatron().getId();
        }
        return user.getId();
    }

    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null) {
            throw new BusinessException("Utilisateur non authentifié");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User introuvable : " + email));
    }

    // ⚠️ MÉTHODE POUR LES STATS GLOBALES (SUPER ADMIN)
    public DashboardStatsDTO getGlobalStats() {
        long nbEntreprises = entrepriseRepository.count();
        long nbClients = clientRepository.count();
        long nbProduits = produitRepository.count();
        long nbCommandes = commandeRepository.count();
        long nbPaiements = paiementRepository.count();
        long nbCommandesAchat = commandeAchatRepository.count();

        BigDecimal caGlobalTTC = commandeRepository.sumGlobalTotalTTC();

        return DashboardStatsDTO.builder()
                .chiffreAffairesTTC(caGlobalTTC != null ? caGlobalTTC : BigDecimal.ZERO)
                .chiffreAffairesHT(BigDecimal.ZERO)
                .margeTotale(BigDecimal.ZERO)
                .nbClients(nbClients)
                .nbProduits(nbProduits)
                .nbCommandes(nbCommandes)
                .nbPaiements(nbPaiements)
                .nbCommandesAchat(nbCommandesAchat)
                .panierMoyen(BigDecimal.valueOf(nbEntreprises)) // Détournement pour afficher le nb d'entreprises
                .build();
    }
}