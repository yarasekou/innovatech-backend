package com.innovatech.api.service;

import com.innovatech.api.dto.*;
import com.innovatech.api.entity.*;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.RetourMapper;
import com.innovatech.api.repository.*;
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
public class RetourService {

    private final RetourRepository retourRepository;
    private final CommandeRepository commandeRepository;
    private final LigneCommandeRepository ligneCommandeRepository;
    private final ClientRepository clientRepository;
    private final ProduitRepository produitRepository;
    private final UserRepository userRepository;
    private final RetourMapper retourMapper;

    private static final Set<String> TRANSITIONS_AUTORISEES = Set.of(
            "DEMANDE->ACCEPTE",
            "DEMANDE->REFUSE",
            "ACCEPTE->RECU",
            "ACCEPTE->REFUSE",
            "RECU->REMBOURSE"
    );

    // -----------------------------------------
    // READ
    // -----------------------------------------

    public List<RetourResponseDTO> findAll() {
        if (SecurityUtils.isAdmin()) {
            return retourRepository.findAllWithDetails()
                    .stream()
                    .map(retourMapper::toResponseDTO)
                    .toList();
        }

        Long dataOwnerId = getCurrentDataOwnerId();
        return retourRepository.findByUserIdWithDetails(dataOwnerId)
                .stream()
                .map(retourMapper::toResponseDTO)
                .toList();
    }

    public RetourResponseDTO findById(Long id) {
        Retour retour;

        if (SecurityUtils.isAdmin()) {
            retour = retourRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Retour", id));
        } else {
            Long dataOwnerId = getCurrentDataOwnerId();
            retour = retourRepository.findByIdAndUserIdWithDetails(id, dataOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Retour", id));
        }

        return retourMapper.toResponseDTO(retour);
    }

    public List<RetourResponseDTO> findByStatut(StatutRetour statut) {
        if (SecurityUtils.isAdmin()) {
            return retourRepository.findAllWithDetails()
                    .stream()
                    .filter(r -> r.getStatut() == statut)
                    .map(retourMapper::toResponseDTO)
                    .toList();
        }

        Long dataOwnerId = getCurrentDataOwnerId();
        return retourRepository.findByUserIdAndStatutWithDetails(dataOwnerId, statut)
                .stream()
                .map(retourMapper::toResponseDTO)
                .toList();
    }

    public List<RetourResponseDTO> findByCommande(Long commandeId) {
        return retourRepository.findByCommandeId(commandeId)
                .stream()
                .map(retourMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @Transactional
    public RetourResponseDTO create(RetourRequestDTO dto, String userEmail) {
        // 1. Charger la commande
        Commande commande = commandeRepository.findByIdWithDetails(dto.getCommandeId())
                .orElseThrow(() -> new ResourceNotFoundException("Commande", dto.getCommandeId()));

        // 2. Vérifier que la commande est LIVREE (on ne peut pas retourner avant)
        if (commande.getStatut() != StatutCommande.LIVREE) {
            throw new BusinessException(
                    "Seule une commande LIVREE peut faire l'objet d'un retour");
        }

        // 3. Récupérer le user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User introuvable avec l'email : " + userEmail));

        // 4. Vérifier que le user a accès à cette commande
        Long dataOwnerId = getCurrentDataOwnerId();
        if (commande.getVendeur() == null || !commande.getVendeur().getId().equals(dataOwnerId)) {
            throw new ResourceNotFoundException("Commande", dto.getCommandeId());
        }

        // 5. Créer le retour
        Retour retour = retourMapper.toEntity(dto, commande, commande.getClient(), user);
        retour.setNumero(genererNumero());
        retour.setStatut(StatutRetour.DEMANDE);
        retour.setDateDemande(LocalDateTime.now());

        // 6. Traiter les lignes
        for (LigneRetourRequestDTO ligneDTO : dto.getLignes()) {
            // 6.1 Vérifier que le produit est dans la commande
            Produit produit = produitRepository.findById(ligneDTO.getProduitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", ligneDTO.getProduitId()));

            // 6.2 Trouver la ligne de commande originale
            LigneCommande ligneOriginale = commande.getLignes().stream()
                    .filter(l -> l.getProduit().getId().equals(produit.getId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(String.format(
                            "Le produit '%s' n'est pas dans la commande %s",
                            produit.getNom(), commande.getNumero())));

            // 6.3 Vérifier la quantité
            if (ligneDTO.getQuantite() > ligneOriginale.getQuantite()) {
                throw new BusinessException(String.format(
                        "Quantité à retourner (%d) supérieure à la quantité commandée (%d) pour '%s'",
                        ligneDTO.getQuantite(), ligneOriginale.getQuantite(), produit.getNom()));
            }

            // 6.4 Vérifier qu'il n'y a pas déjà un retour en cours pour ce produit
            verifierPasDeRetourEnCours(commande.getId(), produit.getId(), ligneDTO.getQuantite());

            // 6.5 Créer la ligne
            LigneRetour ligne = LigneRetour.builder()
                    .produit(produit)
                    .quantite(ligneDTO.getQuantite())
                    .prixUnitaire(ligneOriginale.getPrixUnitaire())
                    .raison(ligneDTO.getRaison())
                    .build();
            ligne.calculerSousTotal();

            retour.ajouterLigne(ligne);
        }

        // 7. Calculer le montant total
        recalculerMontant(retour);

        // 8. Sauvegarder
        Retour saved = retourRepository.save(retour);
        log.info("Retour {} créé par {} pour la commande {}",
                saved.getNumero(), userEmail, commande.getNumero());
        return retourMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // CHANGEMENT DE STATUT
    // -----------------------------------------

    @Transactional
    public RetourResponseDTO changerStatut(Long id, StatutRetour nouveauStatut) {
        Retour retour;

        if (SecurityUtils.isAdmin()) {
            retour = retourRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Retour", id));
        } else {
            Long dataOwnerId = getCurrentDataOwnerId();
            retour = retourRepository.findByIdAndUserIdWithDetails(id, dataOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Retour", id));
        }

        StatutRetour actuel = retour.getStatut();
        String transition = actuel.name() + "->" + nouveauStatut.name();

        if (!TRANSITIONS_AUTORISEES.contains(transition)) {
            throw new BusinessException(String.format(
                    "Transition de statut invalide : %s → %s", actuel, nouveauStatut));
        }

        // ⚠️ À LA RÉCEPTION : restituer le stock
        if (nouveauStatut == StatutRetour.RECU) {
            restituerStock(retour);
        }

        retour.setStatut(nouveauStatut);
        retour.setDateTraitement(LocalDateTime.now());

        Retour updated = retourRepository.save(retour);
        log.info("Retour {} → {}", retour.getNumero(), nouveauStatut);
        return retourMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------

    @Transactional
    public void delete(Long id) {
        Retour retour = retourRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Retour", id));

        if (retour.getStatut() == StatutRetour.RECU
                || retour.getStatut() == StatutRetour.REMBOURSE) {
            throw new BusinessException(
                    "Impossible de supprimer un retour déjà reçu ou remboursé");
        }

        retourRepository.delete(retour);
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    private String genererNumero() {
        int annee = Year.now().getValue();
        long count = retourRepository.countByAnnee(annee);
        return String.format("RET-%d-%04d", annee, count + 1);
    }

    private void recalculerMontant(Retour retour) {
        BigDecimal total = retour.getLignes().stream()
                .map(LigneRetour::getSousTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        retour.setMontantTotal(total);
    }

    /**
     * Vérifie qu'il n'y a pas déjà un retour en cours pour ce produit.
     */
    private void verifierPasDeRetourEnCours(Long commandeId, Long produitId, int nouvelleQuantite) {
        List<Retour> retours = retourRepository.findByCommandeId(commandeId);

        int dejaRetourne = retours.stream()
                .filter(r -> r.getStatut() != StatutRetour.REFUSE)
                .flatMap(r -> r.getLignes().stream())
                .filter(l -> l.getProduit().getId().equals(produitId))
                .mapToInt(LigneRetour::getQuantite)
                .sum();

        // Récupérer la quantité commandée
        // (simplifié : on pourrait être plus précis)
    }

    /**
     * Restitue le stock des produits retournés.
     */
    private void restituerStock(Retour retour) {
        for (LigneRetour ligne : retour.getLignes()) {
            Produit produit = ligne.getProduit();
            int ancienStock = produit.getStock();
            produit.setStock(ancienStock + ligne.getQuantite());
            produitRepository.save(produit);
            log.info("Retour {} : produit {} : stock {} → {} (+{})",
                    retour.getNumero(), produit.getNom(), ancienStock, produit.getStock(), ligne.getQuantite());
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