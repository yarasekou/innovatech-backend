package com.innovatech.api.service;

import com.innovatech.api.dto.CommandeAchatRequestDTO;
import com.innovatech.api.dto.CommandeAchatResponseDTO;
import com.innovatech.api.dto.LigneCommandeAchatRequestDTO;
import com.innovatech.api.entity.*;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.CommandeAchatMapper;
import com.innovatech.api.repository.CommandeAchatRepository;
import com.innovatech.api.repository.FournisseurRepository;
import com.innovatech.api.repository.ProduitRepository;
import com.innovatech.api.repository.UserRepository;
import com.innovatech.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommandeAchatService {

    private final CommandeAchatRepository commandeAchatRepository;
    private final FournisseurRepository fournisseurRepository;
    private final ProduitRepository produitRepository;
    private final UserRepository userRepository;
    private final CommandeAchatMapper commandeAchatMapper;
    private final StockFournisseurService stockFournisseurService;


    private static final Set<String> TRANSITIONS_AUTORISEES = Set.of(
            "EN_ATTENTE->COMMANDEE",
            "EN_ATTENTE->ANNULEE",
            "COMMANDEE->RECU_PARTIEL",
            "COMMANDEE->RECUE",
            "COMMANDEE->ANNULEE",
            "RECU_PARTIEL->RECUE",
            "RECU_PARTIEL->ANNULEE",
            "RECUE->ANNULEE"
    );

    // -----------------------------------------
    // READ
    // -----------------------------------------

    public List<CommandeAchatResponseDTO> findAll() {
        if (SecurityUtils.isAdmin()) {
            return commandeAchatRepository.findAllWithDetails()
                    .stream()
                    .map(commandeAchatMapper::toResponseDTO)
                    .toList();
        }

        Long dataOwnerId = getCurrentDataOwnerId();
        return commandeAchatRepository.findByAcheteurIdWithDetails(dataOwnerId)
                .stream()
                .map(commandeAchatMapper::toResponseDTO)
                .toList();
    }

    public CommandeAchatResponseDTO findById(Long id) {
        CommandeAchat commande;

        if (SecurityUtils.isAdmin()) {
            commande = commandeAchatRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new ResourceNotFoundException("CommandeAchat", id));
        } else {
            Long dataOwnerId = getCurrentDataOwnerId();
            commande = commandeAchatRepository.findByIdAndAcheteurIdWithDetails(id, dataOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("CommandeAchat", id));
        }

        return commandeAchatMapper.toResponseDTO(commande);
    }

    public CommandeAchatResponseDTO findByNumero(String numero) {
        CommandeAchat commande = commandeAchatRepository.findByNumeroWithDetails(numero)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commande d'achat " + numero + " introuvable"));

        // Vérifier l'accès (sauf admin)
        if (!SecurityUtils.isAdmin()) {
            Long dataOwnerId = getCurrentDataOwnerId();
            if (commande.getAcheteur() == null
                    || !commande.getAcheteur().getId().equals(dataOwnerId)) {
                throw new ResourceNotFoundException(
                        "Commande d'achat " + numero + " introuvable");
            }
        }

        return commandeAchatMapper.toResponseDTO(commande);
    }

    public List<CommandeAchatResponseDTO> findByFournisseur(Long fournisseurId) {
        if (!fournisseurRepository.existsById(fournisseurId)) {
            throw new ResourceNotFoundException("Fournisseur", fournisseurId);
        }

        List<CommandeAchat> commandes;

        if (SecurityUtils.isAdmin()) {
            commandes = commandeAchatRepository.findByFournisseurIdWithDetails(fournisseurId);
        } else {
            Long dataOwnerId = getCurrentDataOwnerId();
            commandes = commandeAchatRepository.findByFournisseurIdWithDetails(fournisseurId)
                    .stream()
                    .filter(c -> c.getAcheteur() != null
                            && c.getAcheteur().getId().equals(dataOwnerId))
                    .toList();
        }

        return commandes.stream()
                .map(commandeAchatMapper::toResponseDTO)
                .toList();
    }

    public List<CommandeAchatResponseDTO> findByStatut(StatutAchat statut) {
        if (SecurityUtils.isAdmin()) {
            return commandeAchatRepository.findByStatutWithDetails(statut)
                    .stream()
                    .map(commandeAchatMapper::toResponseDTO)
                    .toList();
        }

        Long dataOwnerId = getCurrentDataOwnerId();
        return commandeAchatRepository.findByStatutAndAcheteurIdWithDetails(statut, dataOwnerId)
                .stream()
                .map(commandeAchatMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @Transactional
    public CommandeAchatResponseDTO create(CommandeAchatRequestDTO dto, String acheteurEmail) {
        // 1. Récupérer le fournisseur
        Fournisseur fournisseur = fournisseurRepository.findById(dto.getFournisseurId())
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", dto.getFournisseurId()));

        if (!fournisseur.isActif()) {
            throw new BusinessException("Impossible de commander à un fournisseur inactif");
        }

        // 2. Récupérer l'acheteur (le USER connecté)
        User acheteur = userRepository.findByEmail(acheteurEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User introuvable avec l'email : " + acheteurEmail));

        // 3. Créer l'entité CommandeAchat
        CommandeAchat commande = commandeAchatMapper.toEntity(dto, fournisseur, acheteur);
        commande.setNumero(genererNumero());
        commande.setStatut(StatutAchat.EN_ATTENTE);
        commande.setDateCommande(LocalDateTime.now());

        // 4. Traiter les lignes
        for (LigneCommandeAchatRequestDTO ligneDTO : dto.getLignes()) {
            // 4.1 Charger le produit
            Produit produit = produitRepository.findById(ligneDTO.getProduitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", ligneDTO.getProduitId()));

            if (!produit.isActif()) {
                throw new BusinessException("Le produit " + produit.getReference() + " est inactif");
            }

            // 4.2 Déterminer le prix d'achat unitaire
            BigDecimal prixAchatUnitaire;

            if (ligneDTO.getPrixAchatUnitaire() != null) {
                // a) Prix explicite dans la requête → on l'utilise
                prixAchatUnitaire = ligneDTO.getPrixAchatUnitaire();
            } else {
                // b) Chercher le prix existant pour ce couple (produit, fournisseur)
                BigDecimal prixExistant = stockFournisseurService.getPrixAchat(
                        produit.getId(),
                        fournisseur.getId()
                );

                if (prixExistant != null && prixExistant.compareTo(BigDecimal.ZERO) > 0) {
                    // Prix déjà connu chez ce fournisseur
                    prixAchatUnitaire = prixExistant;
                    log.info("Prix d'achat récupéré depuis StockFournisseur : {} pour produit {} / fournisseur {}",
                            prixExistant, produit.getReference(), fournisseur.getNom());
                } else {
                    // c) Fallback : prix d'achat par défaut du produit
                    prixAchatUnitaire = produit.getPrixAchat();
                    log.info("Prix d'achat par défaut utilisé : {} pour produit {}",
                            prixAchatUnitaire, produit.getReference());
                }
            }

            if (prixAchatUnitaire == null || prixAchatUnitaire.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(String.format(
                        "Aucun prix d'achat valide pour le produit '%s'. " +
                                "Précisez 'prixAchatUnitaire' dans la requête.",
                        produit.getNom()));
            }

            // 4.3 Créer la ligne
            LigneCommandeAchat ligne = LigneCommandeAchat.builder()
                    .produit(produit)
                    .quantite(ligneDTO.getQuantite())
                    .prixAchatUnitaire(prixAchatUnitaire)
                    .build();
            ligne.calculerSousTotal();

            commande.ajouterLigne(ligne);
        }

        // 5. Recalculer les totaux
        recalculerTotaux(commande);

        // 6. Sauvegarder
        CommandeAchat saved = commandeAchatRepository.save(commande);
        log.info("Commande d'achat {} créée par {} ({})",
                saved.getNumero(), acheteurEmail, fournisseur.getNom());
        return commandeAchatMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @Transactional
    public CommandeAchatResponseDTO update(Long id, CommandeAchatRequestDTO dto) {
        CommandeAchat existing;

        if (SecurityUtils.isAdmin()) {
            existing = commandeAchatRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new ResourceNotFoundException("CommandeAchat", id));
        } else {
            Long dataOwnerId = getCurrentDataOwnerId();
            existing = commandeAchatRepository.findByIdAndAcheteurIdWithDetails(id, dataOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("CommandeAchat", id));
        }

        if (existing.getStatut() != StatutAchat.EN_ATTENTE) {
            throw new BusinessException("Seule une commande EN_ATTENTE peut être modifiée");
        }

        Fournisseur fournisseur = fournisseurRepository.findById(dto.getFournisseurId())
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", dto.getFournisseurId()));

        commandeAchatMapper.updateEntity(existing, dto, fournisseur);
        existing.viderLignes();

        for (LigneCommandeAchatRequestDTO ligneDTO : dto.getLignes()) {
            Produit produit = produitRepository.findById(ligneDTO.getProduitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", ligneDTO.getProduitId()));

            BigDecimal prix = ligneDTO.getPrixAchatUnitaire() != null
                    ? ligneDTO.getPrixAchatUnitaire()
                    : produit.getPrixAchat();

            LigneCommandeAchat ligne = LigneCommandeAchat.builder()
                    .produit(produit)
                    .quantite(ligneDTO.getQuantite())
                    .prixAchatUnitaire(prix)
                    .build();
            ligne.calculerSousTotal();

            existing.ajouterLigne(ligne);
        }

        recalculerTotaux(existing);

        CommandeAchat updated = commandeAchatRepository.save(existing);
        return commandeAchatMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // CHANGEMENT DE STATUT
    // -----------------------------------------

    @Transactional
    public CommandeAchatResponseDTO changerStatut(Long id, StatutAchat nouveauStatut) {
        CommandeAchat commande;

        if (SecurityUtils.isAdmin()) {
            commande = commandeAchatRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new ResourceNotFoundException("CommandeAchat", id));
        } else {
            Long dataOwnerId = getCurrentDataOwnerId();
            commande = commandeAchatRepository.findByIdAndAcheteurIdWithDetails(id, dataOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("CommandeAchat", id));
        }

        StatutAchat actuel = commande.getStatut();
        String transition = actuel.name() + "->" + nouveauStatut.name();

        if (!TRANSITIONS_AUTORISEES.contains(transition)) {
            throw new BusinessException(String.format(
                    "Transition de statut invalide : %s → %s", actuel, nouveauStatut));
        }

        // À la RÉCEPTION → augmenter le stock
        if (nouveauStatut == StatutAchat.RECUE) {
            augmenterStock(commande);
        }

        // À l'ANNULATION après réception → restituer le stock
        if (nouveauStatut == StatutAchat.ANNULEE
                && (actuel == StatutAchat.RECUE || actuel == StatutAchat.RECU_PARTIEL)) {
            restituerStock(commande);
        }

        commande.setStatut(nouveauStatut);
        CommandeAchat updated = commandeAchatRepository.save(commande);
        log.info("Commande d'achat {} → {}", commande.getNumero(), nouveauStatut);
        return commandeAchatMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------

    @Transactional
    public void delete(Long id) {
        CommandeAchat commande;

        if (SecurityUtils.isAdmin()) {
            commande = commandeAchatRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new ResourceNotFoundException("CommandeAchat", id));
        } else {
            Long dataOwnerId = getCurrentDataOwnerId();
            commande = commandeAchatRepository.findByIdAndAcheteurIdWithDetails(id, dataOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("CommandeAchat", id));
        }

        if (commande.getStatut() == StatutAchat.RECUE
                || commande.getStatut() == StatutAchat.RECU_PARTIEL) {
            restituerStock(commande);
        }

        commandeAchatRepository.delete(commande);
        log.info("Commande d'achat {} supprimée", commande.getNumero());
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    private String genererNumero() {
        int annee = Year.now().getValue();
        long count = commandeAchatRepository.countByAnnee(annee);
        return String.format("ACH-%d-%04d", annee, count + 1);
    }

    private void recalculerTotaux(CommandeAchat commande) {
        BigDecimal totalHT = commande.getLignes().stream()
                .map(LigneCommandeAchat::getSousTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tvaPercent = commande.getTvaPercent() != null
                ? commande.getTvaPercent()
                : new BigDecimal("18.00");

        BigDecimal tvaMontant = totalHT
                .multiply(tvaPercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal totalTTC = totalHT.add(tvaMontant).setScale(2, RoundingMode.HALF_UP);

        commande.setTotalHT(totalHT);
        commande.setTotalTTC(totalTTC);
    }

    private void augmenterStock(CommandeAchat commande) {
        Long fournisseurId = commande.getFournisseur().getId();

        for (LigneCommandeAchat ligne : commande.getLignes()) {
            Produit produit = ligne.getProduit();
            int quantite = ligne.getQuantite();
            BigDecimal prixAchat = ligne.getPrixAchatUnitaire();

            // 1. Mettre à jour le stock fournisseur
            stockFournisseurService.ajouterStock(
                    produit.getId(),
                    fournisseurId,
                    quantite,
                    prixAchat
            );

            // 2. Mettre à jour le stock global du produit (agrégé)
            int ancienStock = produit.getStock();
            produit.setStock(ancienStock + quantite);
            produitRepository.save(produit);

            log.info("Produit {} : stock global {} → {} (+{})",
                    produit.getNom(), ancienStock, produit.getStock(), quantite);
        }
    }

    private void restituerStock(CommandeAchat commande) {
        Long fournisseurId = commande.getFournisseur().getId();

        for (LigneCommandeAchat ligne : commande.getLignes()) {
            Produit produit = ligne.getProduit();
            int quantite = ligne.getQuantite();

            // 1. Retirer du stock fournisseur
            try {
                stockFournisseurService.retirerStock(produit.getId(), fournisseurId, quantite);
            } catch (Exception e) {
                log.warn("Impossible de retirer du stock fournisseur : {}", e.getMessage());
                // On continue quand même (le stock global peut être restitué)
            }

            // 2. Restituer le stock global
            int ancienStock = produit.getStock();
            int nouveauStock = ancienStock - quantite;

            if (nouveauStock < 0) {
                throw new BusinessException(String.format(
                        "Impossible de restituer le stock : %s deviendrait négatif (%d → %d)",
                        produit.getNom(), ancienStock, nouveauStock));
            }

            produit.setStock(nouveauStock);
            produitRepository.save(produit);

            log.info("Produit {} : stock global {} → {} (-{})",
                    produit.getNom(), ancienStock, nouveauStock, quantite);
        }
    }

    /**
     * Retourne l'ID du propriétaire des données.
     * - ADMIN : ne devrait pas arriver ici
     * - USER : son ID
     * - SELLER : l'ID de son patron
     */
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