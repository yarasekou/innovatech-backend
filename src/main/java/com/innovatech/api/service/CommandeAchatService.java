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
    private final CommandeAchatMapper commandeAchatMapper;

    private static final Set<String> TRANSITIONS_AUTORISEES = Set.of(
            "EN_ATTENTE->COMMANDEE",
            "EN_ATTENTE->ANNULEE",
            "COMMANDEE->RECU_PARTIEL",
            "COMMANDEE->RECUE",
            "COMMANDEE->ANNULEE",
            "RECU_PARTIEL->RECUE",
            "RECU_PARTIEL->ANNULEE",
            "RECUE->ANNULEE"    // ⚠️ NOUVEAU : on peut annuler une commande reçue
    );

    // -----------------------------------------
    // READ
    // -----------------------------------------

    public List<CommandeAchatResponseDTO> findAll() {
        return commandeAchatRepository.findAllWithDetails()
                .stream().map(commandeAchatMapper::toResponseDTO).toList();
    }

    public CommandeAchatResponseDTO findById(Long id) {
        CommandeAchat c = commandeAchatRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommandeAchat", id));
        return commandeAchatMapper.toResponseDTO(c);
    }

    public CommandeAchatResponseDTO findByNumero(String numero) {
        CommandeAchat c = commandeAchatRepository.findByNumeroWithDetails(numero)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commande d'achat " + numero + " introuvable"));
        return commandeAchatMapper.toResponseDTO(c);
    }

    public List<CommandeAchatResponseDTO> findByFournisseur(Long fournisseurId) {
        if (!fournisseurRepository.existsById(fournisseurId)) {
            throw new ResourceNotFoundException("Fournisseur", fournisseurId);
        }
        return commandeAchatRepository.findByFournisseurIdWithDetails(fournisseurId)
                .stream().map(commandeAchatMapper::toResponseDTO).toList();
    }

    public List<CommandeAchatResponseDTO> findByStatut(StatutAchat statut) {
        return commandeAchatRepository.findByStatutWithDetails(statut)
                .stream().map(commandeAchatMapper::toResponseDTO).toList();
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @Transactional
    public CommandeAchatResponseDTO create(CommandeAchatRequestDTO dto) {
        Fournisseur fournisseur = fournisseurRepository.findById(dto.getFournisseurId())
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", dto.getFournisseurId()));

        if (!fournisseur.isActif()) {
            throw new BusinessException("Impossible de commander à un fournisseur inactif");
        }

        CommandeAchat commande = commandeAchatMapper.toEntity(dto, fournisseur);
        commande.setNumero(genererNumero());
        commande.setStatut(StatutAchat.EN_ATTENTE);
        commande.setDateCommande(LocalDateTime.now());

        for (LigneCommandeAchatRequestDTO ligneDTO : dto.getLignes()) {
            Produit produit = produitRepository.findById(ligneDTO.getProduitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", ligneDTO.getProduitId()));

            if (!produit.isActif()) {
                throw new BusinessException("Le produit " + produit.getReference() + " est inactif");
            }

            BigDecimal prix = ligneDTO.getPrixAchatUnitaire() != null
                    ? ligneDTO.getPrixAchatUnitaire()
                    : produit.getPrixAchat();

            LigneCommandeAchat ligne = LigneCommandeAchat.builder()
                    .produit(produit)
                    .quantite(ligneDTO.getQuantite())
                    .prixAchatUnitaire(prix)
                    .build();
            ligne.calculerSousTotal();

            commande.ajouterLigne(ligne);
        }

        recalculerTotaux(commande);

        CommandeAchat saved = commandeAchatRepository.save(commande);
        log.info("Commande d'achat {} créée pour {}", saved.getNumero(), fournisseur.getNom());
        return commandeAchatMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @Transactional
    public CommandeAchatResponseDTO update(Long id, CommandeAchatRequestDTO dto) {
        CommandeAchat existing = commandeAchatRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommandeAchat", id));

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
        CommandeAchat commande = commandeAchatRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommandeAchat", id));

        StatutAchat actuel = commande.getStatut();
        String transition = actuel.name() + "->" + nouveauStatut.name();

        if (!TRANSITIONS_AUTORISEES.contains(transition)) {
            throw new BusinessException(String.format(
                    "Transition de statut invalide : %s → %s", actuel, nouveauStatut));
        }

        // ⚠️ À la RÉCEPTION → augmenter le stock
        if (nouveauStatut == StatutAchat.RECUE) {
            augmenterStock(commande);
        }

        // ⚠️ À l'ANNULATION après réception → restituer le stock
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
    // DELETE (avec restitution du stock si nécessaire)
    // -----------------------------------------

    @Transactional
    public void delete(Long id) {
        CommandeAchat commande = commandeAchatRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommandeAchat", id));

        // Si la commande a été reçue (totalement ou partiellement),
        // il faut restituer le stock avant suppression.
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

    /**
     * Augmente le stock de chaque produit à la RÉCEPTION.
     */
    private void augmenterStock(CommandeAchat commande) {
        for (LigneCommandeAchat ligne : commande.getLignes()) {
            Produit produit = ligne.getProduit();
            int ancienStock = produit.getStock();
            produit.setStock(ancienStock + ligne.getQuantite());
            produitRepository.save(produit);
            log.info("Produit {} : stock {} → {} (+{})",
                    produit.getNom(), ancienStock, produit.getStock(), ligne.getQuantite());
        }
    }

    /**
     * Diminue le stock (inverse de l'augmentation à la réception).
     * Bloque si le stock deviendrait négatif (marchandise déjà vendue).
     */
    private void restituerStock(CommandeAchat commande) {
        for (LigneCommandeAchat ligne : commande.getLignes()) {
            Produit produit = ligne.getProduit();
            int ancienStock = produit.getStock();
            int nouveauStock = ancienStock - ligne.getQuantite();

            if (nouveauStock < 0) {
                throw new BusinessException(String.format(
                        "Impossible de restituer le stock : %s deviendrait négatif (%d → %d). "
                                + "La marchandise a peut-être déjà été vendue.",
                        produit.getNom(), ancienStock, nouveauStock));
            }

            produit.setStock(nouveauStock);
            produitRepository.save(produit);
            log.info("Produit {} : stock {} → {} (-{})",
                    produit.getNom(), ancienStock, nouveauStock, ligne.getQuantite());
        }
    }
}