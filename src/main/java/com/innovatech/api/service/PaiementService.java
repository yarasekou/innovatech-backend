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

    /**
     * Transitions de statut autorisées.
     */
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
        // SELLER : accès refusé
        if (SecurityUtils.isSeller()) {
            throw new BusinessException("Accès aux paiements non autorisé");
        }

        if (SecurityUtils.isAdmin()) {
            return paiementRepository.findAllWithCommande()
                    .stream()
                    .map(paiementMapper::toResponseDTO)
                    .toList();
        }

        Long dataOwnerId = getCurrentDataOwnerId();
        return paiementRepository.findByUserIdWithDetails(dataOwnerId)
                .stream()
                .map(paiementMapper::toResponseDTO)
                .toList();
    }

    public PaiementResponseDTO findById(Long id) {
        Paiement paiement = paiementRepository.findByIdWithCommande(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement", id));
        return paiementMapper.toResponseDTO(paiement);
    }

    public List<PaiementResponseDTO> findByCommande(Long commandeId) {
        if (!commandeRepository.existsById(commandeId)) {
            throw new ResourceNotFoundException("Commande", commandeId);
        }
        return paiementRepository.findByCommandeIdWithCommande(commandeId)
                .stream()
                .map(paiementMapper::toResponseDTO)
                .toList();
    }

    public List<PaiementResponseDTO> findByStatut(StatutPaiement statut) {
        return paiementRepository.findByStatut(statut)
                .stream()
                .map(paiementMapper::toResponseDTO)
                .toList();
    }

    /**
     * Calcule le reste à payer d'une commande.
     */
    public ResteAPayerDTO calculerReste(Long commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", commandeId));

        return calculerReste(commande);
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @Transactional
    public PaiementResponseDTO create(PaiementRequestDTO dto, String userEmail) {
        Commande commande = commandeRepository.findById(dto.getCommandeId())
                .orElseThrow(() -> new ResourceNotFoundException("Commande", dto.getCommandeId()));

        if (commande.getStatut() == StatutCommande.ANNULEE) {
            throw new BusinessException("Impossible de payer une commande annulée");
        }

        // ⚠️ Vérifier que le user peut payer cette commande
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User introuvable avec l'email : " + userEmail));

        // Un USER peut payer uniquement SES commandes
        // Un SELLER peut payer les commandes de son patron
        Long dataOwnerId = getCurrentDataOwnerId();
        if (!commande.getVendeur().getId().equals(dataOwnerId)) {
            throw new ResourceNotFoundException("Commande", dto.getCommandeId());
        }

        ResteAPayerDTO solde = calculerReste(commande);
        if (dto.getMontant().compareTo(solde.getResteAPayer()) > 0) {
            throw new BusinessException(String.format(
                    "Montant trop élevé. Reste à payer : %.2f, montant demandé : %.2f",
                    solde.getResteAPayer(), dto.getMontant()));
        }

        Paiement paiement = paiementMapper.toEntity(dto, commande, user);
        paiement.setNumero(genererNumero());
        paiement.setStatut(StatutPaiement.VALIDE);
        paiement.setDatePaiement(LocalDateTime.now());

        Paiement saved = paiementRepository.save(paiement);
        log.info("Paiement {} créé par {}", saved.getNumero(), userEmail);

        mettreAJourStatutCommandeSiPayee(commande);
        return paiementMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // CHANGEMENT DE STATUT
    // -----------------------------------------

    @Transactional
    public PaiementResponseDTO changerStatut(Long id, StatutPaiement nouveauStatut) {
        Paiement paiement = paiementRepository.findByIdWithCommande(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement", id));

        StatutPaiement actuel = paiement.getStatut();
        String transition = actuel.name() + "->" + nouveauStatut.name();

        if (!TRANSITIONS_AUTORISEES.contains(transition)) {
            throw new BusinessException(String.format(
                    "Transition de statut invalide : %s → %s", actuel, nouveauStatut));
        }

        paiement.setStatut(nouveauStatut);
        Paiement updated = paiementRepository.save(paiement);

        // Mettre à jour le statut de la commande
        mettreAJourStatutCommandeSiPayee(updated.getCommande());

        return paiementMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------

    @Transactional
    public void delete(Long id) {
        Paiement paiement = paiementRepository.findByIdWithCommande(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement", id));

        if (paiement.getStatut() == StatutPaiement.VALIDE) {
            throw new BusinessException(
                    "Impossible de supprimer un paiement validé. Annulez-le ou remboursez-le.");
        }

        Commande commande = paiement.getCommande();
        paiementRepository.delete(paiement);

        // Vérifier si le statut de la commande doit être modifié
        mettreAJourStatutCommandeSiPayee(commande);
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    private String genererNumero() {
        int annee = Year.now().getValue();
        long count = paiementRepository.countByAnnee(annee);
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

    /**
     * Si la commande est totalement payée, on la passe en VALIDEE.
     * Si elle ne l'est plus, on la repasse en EN_ATTENTE.
     */
    private void mettreAJourStatutCommandeSiPayee(Commande commande) {
        // On ne touche qu'aux commandes en EN_ATTENTE ou VALIDEE
        if (commande.getStatut() != StatutCommande.EN_ATTENTE
                && commande.getStatut() != StatutCommande.VALIDEE) {
            return;
        }

        ResteAPayerDTO solde = calculerReste(commande);

        if (solde.isSolde() && commande.getStatut() == StatutCommande.EN_ATTENTE) {
            commande.setStatut(StatutCommande.VALIDEE);
            commandeRepository.save(commande);
            log.info("Commande {} entièrement payée → statut VALIDEE", commande.getNumero());
        } else if (!solde.isSolde() && commande.getStatut() == StatutCommande.VALIDEE) {
            // Cas rare : remboursement partiel → repasser en EN_ATTENTE
            // (on ne le fait pas car la commande peut déjà être traitée)
            // À voir selon le métier
        }
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