package com.innovatech.api.service;

import com.innovatech.api.dto.FactureRequestDTO;
import com.innovatech.api.dto.FactureResponseDTO;
import com.innovatech.api.entity.*;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.FactureMapper;
import com.innovatech.api.repository.CommandeRepository;
import com.innovatech.api.repository.FactureRepository;
import com.innovatech.api.repository.UserRepository;
import com.innovatech.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        if (SecurityUtils.isAdmin()) {
            return factureRepository.findAllWithDetails()
                    .stream().map(factureMapper::toResponseDTO).toList();
        }
        Long dataOwnerId = getCurrentDataOwnerId();
        return factureRepository.findByVendeurIdWithDetails(dataOwnerId)
                .stream().map(factureMapper::toResponseDTO).toList();
    }

    public FactureResponseDTO findById(Long id) {
        Facture facture;
        if (SecurityUtils.isAdmin()) {
            facture = factureRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Facture", id));
        } else {
            Long dataOwnerId = getCurrentDataOwnerId();
            facture = factureRepository.findByIdAndVendeurIdWithDetails(id, dataOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Facture", id));
        }
        return factureMapper.toResponseDTO(facture);
    }

    public FactureResponseDTO findByNumero(String numero) {
        Facture facture = factureRepository.findByNumero(numero)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Facture avec numéro " + numero + " introuvable"));
        // TODO: vérifier l'accès (comme pour findById)
        return factureMapper.toResponseDTO(facture);
    }

    public FactureResponseDTO findByCommande(Long commandeId) {
        Facture facture = factureRepository.findByCommandeId(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucune facture pour la commande " + commandeId));
        return factureMapper.toResponseDTO(facture);
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @Transactional
    public FactureResponseDTO create(FactureRequestDTO dto) {
        Long dataOwnerId = getCurrentDataOwnerId();

        // 1. Charger la commande
        Commande commande = commandeRepository.findByIdAndVendeurIdWithDetails(
                        dto.getCommandeId(), dataOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", dto.getCommandeId()));

        // 2. Vérifier que la commande est LIVREE
        if (commande.getStatut() != StatutCommande.LIVREE) {
            throw new BusinessException(
                    "Seule une commande LIVREE peut être facturée");
        }

        // 3. Vérifier qu'il n'y a pas déjà une facture
        if (factureRepository.existsByCommandeId(dto.getCommandeId())) {
            throw new BusinessException(
                    "Une facture existe déjà pour la commande " + commande.getNumero());
        }

        // 4. Créer la facture
        LocalDate emission = dto.getDateEmission() != null
                ? dto.getDateEmission()
                : LocalDate.now();

        Facture facture = factureMapper.toEntity(
                commande, genererNumero(), emission, dto.getNotes());

        Facture saved = factureRepository.save(facture);
        log.info("Facture {} créée pour la commande {}",
                saved.getNumero(), commande.getNumero());
        return factureMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // ÉMISSION (génère le PDF)
    // -----------------------------------------

    @Transactional
    public FactureResponseDTO emettre(Long id) {
        Facture facture = findFactureSecurise(id);

        if (facture.getStatut() != StatutFacture.BROUILLON) {
            throw new BusinessException(
                    "Seule une facture BROUILLON peut être émise");
        }

        facture.setStatut(StatutFacture.EMISE);
        facture.setPdfUrl("/api/factures/" + id + "/pdf");

        // TODO: générer le PDF ici (ou dans un endpoint dédié)

        Facture updated = factureRepository.save(facture);
        log.info("Facture {} émise", facture.getNumero());
        return factureMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // PAIEMENT
    // -----------------------------------------

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
            throw new BusinessException(
                    "Impossible de payer une facture non émise. Émettez-la d'abord.");
        }

        facture.setStatut(StatutFacture.PAYEE);
        facture.setDatePaiement(LocalDate.now());

        Facture updated = factureRepository.save(facture);
        log.info("Facture {} marquée PAYEE", facture.getNumero());
        return factureMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // ANNULATION
    // -----------------------------------------

    @Transactional
    public FactureResponseDTO annuler(Long id) {
        Facture facture = findFactureSecurise(id);

        if (facture.getStatut() == StatutFacture.PAYEE) {
            throw new BusinessException(
                    "Impossible d'annuler une facture payée");
        }
        if (facture.getStatut() == StatutFacture.ANNULEE) {
            throw new BusinessException("Cette facture est déjà annulée");
        }

        facture.setStatut(StatutFacture.ANNULEE);
        Facture updated = factureRepository.save(facture);
        log.info("Facture {} annulée", facture.getNumero());
        return factureMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // BATCH : mettre à jour les factures en retard
    // -----------------------------------------

    @Transactional
    public int mettreAJourFacturesEnRetard() {
        List<Facture> enRetard = factureRepository.findFacturesEnRetard(LocalDate.now());
        for (Facture f : enRetard) {
            f.setStatut(StatutFacture.EN_RETARD);
        }
        factureRepository.saveAll(enRetard);
        log.info("{} facture(s) passée(s) en EN_RETARD", enRetard.size());
        return enRetard.size();
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------

    @Transactional
    public void delete(Long id) {
        Facture facture = findFactureSecurise(id);

        if (facture.getStatut() != StatutFacture.BROUILLON) {
            throw new BusinessException(
                    "Seule une facture BROUILLON peut être supprimée. Annulez-la à la place.");
        }

        factureRepository.delete(facture);
        log.info("Facture {} supprimée", facture.getNumero());
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    private Facture findFactureSecurise(Long id) {
        if (SecurityUtils.isAdmin()) {
            return factureRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Facture", id));
        }
        Long dataOwnerId = getCurrentDataOwnerId();
        return factureRepository.findByIdAndVendeurIdWithDetails(id, dataOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture", id));
    }

    private String genererNumero() {
        int annee = Year.now().getValue();
        long count = factureRepository.countByAnnee(annee);
        return String.format("FAC-%d-%04d", annee, count + 1);
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