package com.innovatech.api.service;

import com.innovatech.api.dto.dashboard.*;
import com.innovatech.api.entity.Entreprise;
import com.innovatech.api.entity.Role;
import com.innovatech.api.entity.User;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.repository.*;
import com.innovatech.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private static final String[] MOIS_NOMS = {
            "", "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    };

    // -----------------------------------------
    // STATS GLOBALES
    // -----------------------------------------

    public DashboardStatsDTO getStats() {
        User user = getCurrentUser();

        if (user.hasRole(Role.ROLE_SUPER_ADMIN)) {
            return getStatsForSuperAdmin();
        }

        if (user.getEntreprise() == null) {
            throw new BusinessException(
                    "Aucune entreprise associée. Contactez votre administrateur.");
        }

        Long entrepriseId = user.getEntreprise().getId();

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return getStatsForEntreprise(entrepriseId);
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return getStatsForBoutique(entrepriseId, shopOwnerId);
    }

    private DashboardStatsDTO getStatsForSuperAdmin() {
        return DashboardStatsDTO.builder()
                .chiffreAffairesHT(BigDecimal.ZERO)
                .chiffreAffairesTTC(BigDecimal.ZERO)
                .margeTotale(BigDecimal.ZERO)
                .nbClients(clientRepository.count())
                .nbProduits(produitRepository.count())
                .nbCommandes(0L)
                .nbPaiements(paiementRepository.count())
                .nbCommandesAchat(commandeAchatRepository.count())
                .nbCommandesEnAttente(0L)
                .nbCommandesValidees(0L)
                .nbCommandesEnCours(0L)
                .nbCommandesLivrees(0L)
                .nbCommandesAnnulees(0L)
                .panierMoyen(BigDecimal.ZERO)
                .build();
    }

    private DashboardStatsDTO getStatsForEntreprise(Long entrepriseId) {
        return DashboardStatsDTO.builder()
                .chiffreAffairesHT(BigDecimal.ZERO)
                .chiffreAffairesTTC(BigDecimal.ZERO)
                .margeTotale(BigDecimal.ZERO)
                .nbClients(clientRepository.countByEntrepriseId(entrepriseId))
                .nbProduits(produitRepository.countByEntrepriseId(entrepriseId))
                .nbCommandes(commandeRepository.countByEntrepriseId(entrepriseId))
                .nbPaiements(0L)                                       // TODO
                .nbCommandesAchat(0L)                                  // TODO
                .nbCommandesEnAttente(0L)                              // TODO
                .nbCommandesValidees(0L)
                .nbCommandesEnCours(0L)
                .nbCommandesLivrees(0L)
                .nbCommandesAnnulees(0L)
                .panierMoyen(BigDecimal.ZERO)
                .build();
    }

    private DashboardStatsDTO getStatsForBoutique(Long entrepriseId, Long shopOwnerId) {
        return DashboardStatsDTO.builder()
                .chiffreAffairesHT(BigDecimal.ZERO)
                .chiffreAffairesTTC(BigDecimal.ZERO)
                .margeTotale(BigDecimal.ZERO)
                .nbClients(clientRepository.countByEntrepriseIdAndUserId(entrepriseId, shopOwnerId))
                .nbProduits(produitRepository.countByEntrepriseIdAndUserId(entrepriseId, shopOwnerId))
                .nbCommandes(commandeRepository.countByEntrepriseIdAndUserId(entrepriseId, shopOwnerId))
                .nbPaiements(0L)
                .nbCommandesAchat(0L)
                .nbCommandesEnAttente(0L)
                .nbCommandesValidees(0L)
                .nbCommandesEnCours(0L)
                .nbCommandesLivrees(0L)
                .nbCommandesAnnulees(0L)
                .panierMoyen(BigDecimal.ZERO)
                .build();
    }

    // -----------------------------------------
    // TOP (temporaire : global)
    // -----------------------------------------

    public List<TopProduitDTO> getTopProduits(int limit) {
        return List.of();   // TODO : implémenter avec scoping
    }

    public List<TopClientDTO> getTopClients(int limit) {
        return List.of();   // TODO : implémenter avec scoping
    }

    public List<ChiffreAffairesMoisDTO> getChiffreAffairesMensuel() {
        return List.of();   // TODO : implémenter avec scoping
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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User introuvable avec l'email : " + email));
    }
}