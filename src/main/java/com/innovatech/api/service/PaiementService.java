package com.innovatech.api.service;

import com.innovatech.api.dto.PaiementRequestDTO;
import com.innovatech.api.dto.PaiementResponseDTO;
import com.innovatech.api.dto.ResteAPayerDTO;
import com.innovatech.api.entity.*;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.PaiementMapper;
import com.innovatech.api.repository.CommandeRepository;
import com.innovatech.api.repository.PaiementRepository;
import com.innovatech.api.repository.UserRepository;
import com.innovatech.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaiementService {

    private final PaiementRepository paiementRepository;
    private final CommandeRepository commandeRepository;
    private final PaiementMapper paiementMapper;
    private final UserRepository userRepository;

    private static final Set<String> TRANSITIONS_AUTORISEES = Set.of(
            "EN_ATTENTE->VALIDE",
            "EN_ATTENTE->ANNULE",
            "VALIDE->REMBOURSE",
            "VALIDE->ANNULE"
    );

    // -----------------------------------------
    // READ
    // -----------------------------------------

    public List<PaiementResponseDTO> findAll() {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return paiementRepository.findByEntrepriseIdWithDetails(entreprise.getId())
                    .stream()
                    .map(paiementMapper::toResponseDTO)
                    .toList();
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return paiementRepository.findByEntrepriseIdAndUserIdWithDetails(
                        entreprise.getId(), shopOwnerId)
                .stream()
                .map(paiementMapper::toResponseDTO)
                .toList();
    }

    public PaiementResponseDTO findById(Long id) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Paiement paiement;

        if (user.hasRole(Role.ROLE_ADMIN)) {
            paiement = paiementRepository
                    .findByIdAndEntrepriseIdWithDetails(id, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Paiement", id));
        } else {
            Long shopOwnerId = resolveShopOwnerId(user);
            paiement = paiementRepository
                    .findByIdAndEntrepriseIdAndUserIdWithDetails(
                            id, entreprise.getId(), shopOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Paiement", id));
        }

        return paiementMapper.toResponseDTO(paiement);
    }

    public List<PaiementResponseDTO> findByCommande(Long commandeId) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Commande commande = findMyCommande(user, entreprise, commandeId);

        return paiementRepository.findByCommandeIdWithDetails(commande.getId())
                .stream()
                .map(paiementMapper::toResponseDTO)
                .toList();
    }

    public List<PaiementResponseDTO> findByStatut(StatutPaiement statut) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return paiementRepository.findByEntrepriseIdAndStatutWithDetails(
                            entreprise.getId(), statut)
                    .stream()
                    .map(paiementMapper::toResponseDTO)
                    .toList();
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return paiementRepository.findByEntrepriseIdAndUserIdAndStatutWithDetails(
                        entreprise.getId(), shopOwnerId, statut)
                .stream()
                .map(paiementMapper::toResponseDTO)
                .toList();
    }

    public ResteAPayerDTO calculerReste(Long commandeId) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Commande commande = findMyCommande(user, entreprise, commandeId);
        return calculerReste(commande);
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @Transactional
    public PaiementResponseDTO create(PaiementRequestDTO dto, String userEmail) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            throw new BusinessException(
                    "Un ADMIN ne crée pas de paiements. Créez un USER pour cela.");
        }

        Commande commande = findMyCommande(user, entreprise, dto.getCommandeId());

        if (commande.getStatut() == StatutCommande.ANNULEE) {
            throw new BusinessException("Impossible de payer une commande annulée");
        }

        ResteAPayerDTO solde = calculerReste(commande);
        if (dto.getMontant().compareTo(solde.getResteAPayer()) > 0) {
            throw new BusinessException(String.format(
                    "Montant trop élevé. Reste à payer : %.2f, montant demandé : %.2f",
                    solde.getResteAPayer(), dto.getMontant()));
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        User shopOwner = userRepository.findById(shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("User (boutique)", shopOwnerId));

        Paiement paiement = paiementMapper.toEntity(dto, commande, entreprise, shopOwner);
        paiement.setNumero(genererNumero(entreprise.getId(), shopOwnerId));
        paiement.setStatut(StatutPaiement.VALIDE);
        paiement.setDatePaiement(LocalDateTime.now());

        Paiement saved = paiementRepository.save(paiement);
        log.info("Paiement {} créé par {} pour la commande {}",
                saved.getNumero(), userEmail, commande.getNumero());

        mettreAJourStatutCommandeSiPayee(commande);
        return paiementMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // CHANGEMENT DE STATUT
    // -----------------------------------------

    @Transactional
    public PaiementResponseDTO changerStatut(Long id, StatutPaiement nouveauStatut) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Paiement paiement = findMyPaiement(user, entreprise, id);

        StatutPaiement actuel = paiement.getStatut();
        String transition = actuel.name() + "->" + nouveauStatut.name();

        if (!TRANSITIONS_AUTORISEES.contains(transition)) {
            throw new BusinessException(String.format(
                    "Transition de statut invalide : %s → %s", actuel, nouveauStatut));
        }

        paiement.setStatut(nouveauStatut);
        Paiement updated = paiementRepository.save(paiement);

        mettreAJourStatutCommandeSiPayee(updated.getCommande());
        return paiementMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------

    @Transactional
    public void delete(Long id) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Paiement paiement = findMyPaiement(user, entreprise, id);

        if (paiement.getStatut() == StatutPaiement.VALIDE) {
            throw new BusinessException(
                    "Impossible de supprimer un paiement validé. Annulez-le ou remboursez-le.");
        }

        Commande commande = paiement.getCommande();
        paiementRepository.delete(paiement);
        mettreAJourStatutCommandeSiPayee(commande);
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    private String genererNumero(Long entrepriseId, Long userId) {
        int annee = Year.now().getValue();
        long count = paiementRepository.countByAnneeAndEntrepriseIdAndUserId(
                annee, entrepriseId, userId);
        return String.format("PAY-%d-%04d", annee, count + 1);
    }

    private ResteAPayerDTO calculerReste(Commande commande) {
        BigDecimal totalPaye = paiementRepository
                .sumMontantValideByCommandeId(commande.getId());

        BigDecimal totalTTC = commande.getTotalTTC() != null
                ? commande.getTotalTTC()
                : BigDecimal.ZERO;

        BigDecimal reste = totalTTC.subtract(totalPaye);

        return ResteAPayerDTO.builder()
                .commandeId(commande.getId())
                .commandeNumero(commande.getNumero())
                .totalTTC(totalTTC)
                .totalPaye(totalPaye)
                .resteAPayer(reste.max(BigDecimal.ZERO))
                .solde(reste.compareTo(BigDecimal.ZERO) <= 0)
                .build();
    }

    private void mettreAJourStatutCommandeSiPayee(Commande commande) {
        if (commande.getStatut() != StatutCommande.EN_ATTENTE
                && commande.getStatut() != StatutCommande.VALIDEE) {
            return;
        }

        ResteAPayerDTO solde = calculerReste(commande);

        if (solde.isSolde() && commande.getStatut() == StatutCommande.EN_ATTENTE) {
            commande.setStatut(StatutCommande.VALIDEE);
            commandeRepository.save(commande);
            log.info("Commande {} entièrement payée → VALIDEE", commande.getNumero());
        }
    }

    private Paiement findMyPaiement(User user, Entreprise entreprise, Long id) {
        if (user.hasRole(Role.ROLE_ADMIN)) {
            return paiementRepository
                    .findByIdAndEntrepriseIdWithDetails(id, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Paiement", id));
        }
        Long shopOwnerId = resolveShopOwnerId(user);
        return paiementRepository
                .findByIdAndEntrepriseIdAndUserIdWithDetails(
                        id, entreprise.getId(), shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement", id));
    }

    private Commande findMyCommande(User user, Entreprise entreprise, Long commandeId) {
        if (user.hasRole(Role.ROLE_ADMIN)) {
            return commandeRepository
                    .findByIdAndEntrepriseIdWithDetails(commandeId, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Commande", commandeId));
        }
        Long shopOwnerId = resolveShopOwnerId(user);
        return commandeRepository
                .findByIdAndEntrepriseIdAndUserIdWithDetails(
                        commandeId, entreprise.getId(), shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", commandeId));
    }

    private Long resolveShopOwnerId(User user) {
        if (user.hasRole(Role.ROLE_SELLER)) {
            if (user.getPatron() == null) {
                throw new BusinessException("Ce SELLER n'a pas de patron assigné");
            }
            return user.getPatron().getId();
        }
        return user.getId();
    }

    private Entreprise getCurrentEntreprise(User user) {
        if (user.getEntreprise() == null) {
            throw new BusinessException(
                    "Aucune entreprise associée. Contactez votre administrateur.");
        }
        return user.getEntreprise();
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