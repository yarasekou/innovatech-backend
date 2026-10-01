package com.innovatech.api.service;

import com.innovatech.api.dto.dashboard.*;
import com.innovatech.api.entity.Role;
import com.innovatech.api.entity.StatutCommande;
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

    // Noms des mois pour le libellé
    private static final String[] MOIS_NOMS = {
            "", "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    };

    // -----------------------------------------
    // STATS GLOBALES
    // -----------------------------------------

    public DashboardStatsDTO getStats() {
        if (SecurityUtils.isAdmin()) {
            return getStatsForAdmin();
        }
        return getStatsForUser(getCurrentDataOwnerId());
    }

    /**
     * Stats complètes (vue ADMIN).
     */
    private DashboardStatsDTO getStatsForAdmin() {
        long nbCommandes = commandeRepository.countNonAnnulees();
        BigDecimal caHT = commandeRepository.sumTotalHT();
        BigDecimal caTTC = commandeRepository.sumTotalTTC();
        BigDecimal panierMoyen = calculerPanierMoyen(caTTC, nbCommandes);

        return DashboardStatsDTO.builder()
                .chiffreAffairesHT(caHT)
                .chiffreAffairesTTC(caTTC)
                .margeTotale(caHT)   // Approximation : marge brute = CA HT
                .nbClients(clientRepository.count())
                .nbProduits(produitRepository.count())
                .nbCommandes(nbCommandes)
                .nbPaiements(paiementRepository.count())
                .nbCommandesAchat(commandeAchatRepository.count())
                .nbCommandesEnAttente(commandeRepository.countByStatut(StatutCommande.EN_ATTENTE))
                .nbCommandesValidees(commandeRepository.countByStatut(StatutCommande.VALIDEE))
                .nbCommandesEnCours(commandeRepository.countByStatut(StatutCommande.EN_COURS))
                .nbCommandesLivrees(commandeRepository.countByStatut(StatutCommande.LIVREE))
                .nbCommandesAnnulees(commandeRepository.countByStatut(StatutCommande.ANNULEE))
                .panierMoyen(panierMoyen)
                .build();
    }

    /**
     * Stats filtrées par user (vue USER).
     */
    private DashboardStatsDTO getStatsForUser(Long userId) {
        // Pour l'instant, on utilise les mêmes méthodes globales
        // Une V2 pourrait filtrer par user_id sur chaque entité
        // Pour l'instant : retourne les stats de l'user
        // (à compléter si besoin)
        return DashboardStatsDTO.builder()
                .chiffreAffairesHT(BigDecimal.ZERO)
                .chiffreAffairesTTC(BigDecimal.ZERO)
                .margeTotale(BigDecimal.ZERO)
                .nbClients(clientRepository.countByUserId(userId))
                .nbProduits(produitRepository.countByUserId(userId))
                .nbCommandes(0L)
                .nbPaiements(paiementRepository.countValideByUserId(userId))
                .nbCommandesAchat(commandeAchatRepository.countByAcheteurId(userId))
                .nbCommandesEnAttente(0L)
                .nbCommandesValidees(0L)
                .nbCommandesEnCours(0L)
                .nbCommandesLivrees(0L)
                .nbCommandesAnnulees(0L)
                .panierMoyen(BigDecimal.ZERO)
                .build();
    }

    // -----------------------------------------
    // TOP PRODUITS
    // -----------------------------------------

    public List<TopProduitDTO> getTopProduits(int limit) {
        return commandeRepository.findTopProduits()
                .stream()
                .limit(limit)
                .toList();
    }

    // -----------------------------------------
    // TOP CLIENTS
    // -----------------------------------------

    public List<TopClientDTO> getTopClients(int limit) {
        return commandeRepository.findTopClients()
                .stream()
                .limit(limit)
                .toList();
    }

    // -----------------------------------------
    // CA MENSUEL
    // -----------------------------------------

    public List<ChiffreAffairesMoisDTO> getChiffreAffairesMensuel() {
        List<ChiffreAffairesMoisDTO> resultats = commandeRepository.findChiffreAffairesParMois();

        // Générer le libellé côté Java
        for (ChiffreAffairesMoisDTO dto : resultats) {
            if (dto.getMois() != null && dto.getMois() >= 1 && dto.getMois() <= 12) {
                dto.setLibelle(MOIS_NOMS[dto.getMois()] + " " + dto.getAnnee());
            }
        }

        return resultats;
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    private BigDecimal calculerPanierMoyen(BigDecimal caTTC, long nbCommandes) {
        if (nbCommandes == 0) {
            return BigDecimal.ZERO;
        }
        return caTTC.divide(BigDecimal.valueOf(nbCommandes), 2, RoundingMode.HALF_UP);
    }

    private Long getCurrentDataOwnerId() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null) {
            throw new BusinessException("Utilisateur non authentifié");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User introuvable avec l'email : " + email));

        if (user.getRoles().contains(Role.ROLE_SELLER)) {
            if (user.getPatron() == null) {
                throw new BusinessException("Ce SELLER n'a pas de patron assigné");
            }
            return user.getPatron().getId();
        }

        return user.getId();
    }
}