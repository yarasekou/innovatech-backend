package com.innovatech.api.service;

import com.innovatech.api.dto.FactureRequestDTO;
import com.innovatech.api.dto.FactureResponseDTO;
import com.innovatech.api.entity.*;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.FactureMapper;
import com.innovatech.api.repository.CommandeAchatRepository;
import com.innovatech.api.repository.CommandeRepository;
import com.innovatech.api.repository.FactureRepository;
import com.innovatech.api.repository.UserRepository;
import com.innovatech.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FactureService {

    private final FactureRepository factureRepository;
    private final CommandeRepository commandeRepository;
    private final CommandeAchatRepository commandeAchatRepository; // ⚠️ NOUVEAU
    private final UserRepository userRepository;
    private final FactureMapper factureMapper;

    private static final Set<String> TRANSITIONS_AUTORISEES = Set.of(
            "BROUILLON->EMISE",
            "BROUILLON->ANNULEE",
            "EMISE->PAYEE",
            "EMISE->EN_RETARD",
            "EMISE->ANNULEE",
            "EN_RETARD->PAYEE",
            "EN_RETARD->ANNULEE"
    );

    // -----------------------------------------
    // READ
    // -----------------------------------------

    public List<FactureResponseDTO> findAll() {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return factureRepository.findByEntrepriseIdWithDetails(entreprise.getId())
                    .stream().map(factureMapper::toResponseDTO).toList();
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return factureRepository.findByEntrepriseIdAndUserIdWithDetails(
                        entreprise.getId(), shopOwnerId)
                .stream().map(factureMapper::toResponseDTO).toList();
    }

    public FactureResponseDTO findById(Long id) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Facture facture;

        if (user.hasRole(Role.ROLE_ADMIN)) {
            facture = factureRepository
                    .findByIdAndEntrepriseIdWithDetails(id, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Facture", id));
        } else {
            Long shopOwnerId = resolveShopOwnerId(user);
            facture = factureRepository
                    .findByIdAndEntrepriseIdAndUserIdWithDetails(
                            id, entreprise.getId(), shopOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Facture", id));
        }

        return factureMapper.toResponseDTO(facture);
    }

    public FactureResponseDTO findByCommande(Long commandeId) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Commande commande = findMyCommande(user, entreprise, commandeId);

        Facture facture = factureRepository.findByCommandeId(commande.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucune facture pour la commande " + commandeId));
        return factureMapper.toResponseDTO(facture);
    }

    // ⚠️ NOUVEAU : Par commande d'achat
    public FactureResponseDTO findByCommandeAchat(Long commandeAchatId) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        CommandeAchat commandeAchat = findMyCommandeAchat(user, entreprise, commandeAchatId);

        Facture facture = factureRepository.findByCommandeAchatId(commandeAchat.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucune facture pour la commande d'achat " + commandeAchatId));
        return factureMapper.toResponseDTO(facture);
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @Transactional
    public FactureResponseDTO create(FactureRequestDTO dto) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            throw new BusinessException("Un ADMIN ne crée pas de factures");
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        User shopOwner = userRepository.findById(shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("User (boutique)", shopOwnerId));

        LocalDate emission = dto.getDateEmission() != null ? dto.getDateEmission() : LocalDate.now();
        Facture facture;

        // CAS 1 : FACTURE POUR UN ACHAT
        if (dto.getCommandeAchatId() != null) {
            CommandeAchat achat = findMyCommandeAchat(user, entreprise, dto.getCommandeAchatId());

            if (achat.getStatut() != StatutAchat.RECUE) {
                throw new BusinessException("Seule une commande d'achat REÇUE peut être facturée");
            }

            if (factureRepository.existsByCommandeAchatId(dto.getCommandeAchatId())) {
                throw new BusinessException("Une facture existe déjà pour cette commande d'achat");
            }

            facture = factureMapper.toEntityAchat(
                    achat, entreprise, shopOwner,
                    genererNumeroAchat(entreprise.getId(), shopOwnerId),
                    emission, dto.getNotes());
        }
        // CAS 2 : FACTURE POUR UNE VENTE
        else {
            if (dto.getCommandeId() == null) {
                throw new BusinessException("Il faut fournir commandeId ou commandeAchatId");
            }

            Commande commande = findMyCommande(user, entreprise, dto.getCommandeId());

            if (commande.getStatut() != StatutCommande.LIVREE) {
                throw new BusinessException("Seule une commande LIVREE peut être facturée");
            }

            if (factureRepository.existsByCommandeId(dto.getCommandeId())) {
                throw new BusinessException("Une facture existe déjà pour cette commande");
            }

            facture = factureMapper.toEntity(
                    commande, entreprise, shopOwner,
                    genererNumero(entreprise.getId(), shopOwnerId),
                    emission, dto.getNotes());
        }

        Facture saved = factureRepository.save(facture);
        log.info("Facture {} créée", saved.getNumero());
        return factureMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // CYCLE DE VIE
    // -----------------------------------------

    @Transactional
    public FactureResponseDTO emettre(Long id) {
        Facture facture = findFactureSecurise(id);

        if (facture.getStatut() != StatutFacture.BROUILLON) {
            throw new BusinessException("Seule une facture BROUILLON peut être émise");
        }

        facture.setStatut(StatutFacture.EMISE);
        facture.setPdfUrl("/api/factures/" + id + "/pdf");

        Facture updated = factureRepository.save(facture);
        return factureMapper.toResponseDTO(updated);
    }

    @Transactional
    public FactureResponseDTO marquerPayee(Long id) {
        Facture facture = findFactureSecurise(id);

        if (facture.getStatut() == StatutFacture.PAYEE) {
            throw new BusinessException("Cette facture est déjà payée");
        }
        if (facture.getStatut() == StatutFacture.ANNULEE) {
            throw new BusinessException("Impossible de payer une facture annulée");
        }
        if (facture.getStatut() == StatutFacture.BROUILLON) {
            throw new BusinessException("Impossible de payer une facture non émise");
        }

        facture.setStatut(StatutFacture.PAYEE);
        facture.setDatePaiement(LocalDate.now());

        Facture updated = factureRepository.save(facture);
        return factureMapper.toResponseDTO(updated);
    }

    @Transactional
    public FactureResponseDTO annuler(Long id) {
        Facture facture = findFactureSecurise(id);

        if (facture.getStatut() == StatutFacture.PAYEE) {
            throw new BusinessException("Impossible d'annuler une facture payée");
        }
        if (facture.getStatut() == StatutFacture.ANNULEE) {
            throw new BusinessException("Cette facture est déjà annulée");
        }

        facture.setStatut(StatutFacture.ANNULEE);
        Facture updated = factureRepository.save(facture);
        return factureMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // BATCH
    // -----------------------------------------

    @Transactional
    public int mettreAJourFacturesEnRetard() {
        List<Facture> enRetard = factureRepository.findFacturesEnRetard(LocalDate.now());
        for (Facture f : enRetard) {
            f.setStatut(StatutFacture.EN_RETARD);
        }
        factureRepository.saveAll(enRetard);
        return enRetard.size();
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------

    @Transactional
    public void delete(Long id) {
        Facture facture = findFactureSecurise(id);

        if (facture.getStatut() != StatutFacture.BROUILLON) {
            throw new BusinessException("Seule une facture BROUILLON peut être supprimée");
        }

        factureRepository.delete(facture);
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    private Facture findFactureSecurise(Long id) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return factureRepository
                    .findByIdAndEntrepriseIdWithDetails(id, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Facture", id));
        }
        Long shopOwnerId = resolveShopOwnerId(user);
        return factureRepository
                .findByIdAndEntrepriseIdAndUserIdWithDetails(
                        id, entreprise.getId(), shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture", id));
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

    // ⚠️ NOUVEAU
    private CommandeAchat findMyCommandeAchat(User user, Entreprise entreprise, Long commandeAchatId) {
        if (user.hasRole(Role.ROLE_ADMIN)) {
            return commandeAchatRepository
                    .findByIdAndEntrepriseIdWithDetails(commandeAchatId, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("CommandeAchat", commandeAchatId));
        }
        Long shopOwnerId = resolveShopOwnerId(user);
        return commandeAchatRepository
                .findByIdAndEntrepriseIdAndUserIdWithDetails(
                        commandeAchatId, entreprise.getId(), shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("CommandeAchat", commandeAchatId));
    }

    private String genererNumero(Long entrepriseId, Long userId) {
        int annee = Year.now().getValue();
        long count = factureRepository.countByAnneeAndEntrepriseIdAndUserId(annee, entrepriseId, userId);
        return String.format("FAC-%d-%04d", annee, count + 1);
    }

    private String genererNumeroAchat(Long entrepriseId, Long userId) {
        int annee = Year.now().getValue();
        long count = factureRepository.countByAnneeAndEntrepriseIdAndUserId(annee, entrepriseId, userId);
        return String.format("FAC-ACH-%d-%04d", annee, count + 1);
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
            throw new BusinessException("Aucune entreprise associée. Contactez votre administrateur.");
        }
        return user.getEntreprise();
    }

    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null) {
            throw new BusinessException("Utilisateur non authentifié");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User introuvable avec l'email : " + email));
    }
}