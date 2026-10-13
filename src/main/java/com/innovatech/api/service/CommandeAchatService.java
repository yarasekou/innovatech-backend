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
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return commandeAchatRepository.findByEntrepriseIdWithDetails(entreprise.getId())
                    .stream().map(commandeAchatMapper::toResponseDTO).toList();
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return commandeAchatRepository.findByEntrepriseIdAndUserIdWithDetails(
                        entreprise.getId(), shopOwnerId)
                .stream().map(commandeAchatMapper::toResponseDTO).toList();
    }

    public CommandeAchatResponseDTO findById(Long id) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        CommandeAchat commande;

        if (user.hasRole(Role.ROLE_ADMIN)) {
            commande = commandeAchatRepository
                    .findByIdAndEntrepriseIdWithDetails(id, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("CommandeAchat", id));
        } else {
            Long shopOwnerId = resolveShopOwnerId(user);
            commande = commandeAchatRepository
                    .findByIdAndEntrepriseIdAndUserIdWithDetails(
                            id, entreprise.getId(), shopOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("CommandeAchat", id));
        }

        return commandeAchatMapper.toResponseDTO(commande);
    }

    public List<CommandeAchatResponseDTO> findByFournisseur(Long fournisseurId) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        // Vérifier que le fournisseur appartient à la boutique
        Fournisseur fournisseur = findMyFournisseur(user, entreprise, fournisseurId);

        return commandeAchatRepository.findByFournisseurIdWithDetails(fournisseur.getId())
                .stream().map(commandeAchatMapper::toResponseDTO).toList();
    }

    public List<CommandeAchatResponseDTO> findByStatut(StatutAchat statut) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return commandeAchatRepository.findByEntrepriseIdAndStatutWithDetails(
                            entreprise.getId(), statut)
                    .stream().map(commandeAchatMapper::toResponseDTO).toList();
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return commandeAchatRepository.findByEntrepriseIdAndUserIdAndStatutWithDetails(
                        entreprise.getId(), shopOwnerId, statut)
                .stream().map(commandeAchatMapper::toResponseDTO).toList();
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @Transactional
    public CommandeAchatResponseDTO create(CommandeAchatRequestDTO dto, String userEmail) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            throw new BusinessException("Un ADMIN ne crée pas de commandes d'achat");
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        User shopOwner = userRepository.findById(shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("User (boutique)", shopOwnerId));

        // Vérifier que le fournisseur appartient à la boutique
        Fournisseur fournisseur = findMyFournisseur(user, entreprise, dto.getFournisseurId());

        if (!fournisseur.isActif()) {
            throw new BusinessException("Impossible de commander à un fournisseur inactif");
        }

        CommandeAchat commande = commandeAchatMapper.toEntity(
                dto, entreprise, shopOwner, fournisseur);
        commande.setNumero(genererNumero(entreprise.getId(), shopOwnerId));
        commande.setStatut(StatutAchat.EN_ATTENTE);
        commande.setDateCommande(LocalDateTime.now());

        for (LigneCommandeAchatRequestDTO ligneDTO : dto.getLignes()) {
            // Vérifier que le produit appartient à la boutique
            Produit produit = produitRepository
                    .findByIdAndEntrepriseIdAndUserIdWithDetails(
                            ligneDTO.getProduitId(), entreprise.getId(), shopOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Produit", ligneDTO.getProduitId()));

            if (!produit.isActif()) {
                throw new BusinessException("Le produit " + produit.getReference() + " est inactif");
            }

            BigDecimal prixAchatUnitaire;
            if (ligneDTO.getPrixAchatUnitaire() != null) {
                prixAchatUnitaire = ligneDTO.getPrixAchatUnitaire();
            } else {
                BigDecimal prixExistant = stockFournisseurService.getPrixAchat(
                        produit.getId(), fournisseur.getId());
                if (prixExistant != null && prixExistant.compareTo(BigDecimal.ZERO) > 0) {
                    prixAchatUnitaire = prixExistant;
                } else {
                    prixAchatUnitaire = produit.getPrixAchat();
                }
            }

            if (prixAchatUnitaire == null || prixAchatUnitaire.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(String.format(
                        "Aucun prix d'achat valide pour le produit '%s'", produit.getNom()));
            }

            LigneCommandeAchat ligne = LigneCommandeAchat.builder()
                    .produit(produit)
                    .quantite(ligneDTO.getQuantite())
                    .prixAchatUnitaire(prixAchatUnitaire)
                    .build();
            ligne.calculerSousTotal();

            commande.ajouterLigne(ligne);
        }

        recalculerTotaux(commande);

        CommandeAchat saved = commandeAchatRepository.save(commande);
        log.info("Commande d'achat {} créée par {} pour la boutique de {}",
                saved.getNumero(), userEmail, shopOwner.getEmail());
        return commandeAchatMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @Transactional
    public CommandeAchatResponseDTO update(Long id, CommandeAchatRequestDTO dto) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        CommandeAchat existing = findMyCommande(user, entreprise, id);

        if (existing.getStatut() != StatutAchat.EN_ATTENTE) {
            throw new BusinessException("Seule une commande EN_ATTENTE peut être modifiée");
        }

        Fournisseur fournisseur = findMyFournisseur(user, entreprise, dto.getFournisseurId());

        commandeAchatMapper.updateEntity(existing, dto, fournisseur);
        existing.viderLignes();

        Long shopOwnerId = existing.getUser().getId();

        for (LigneCommandeAchatRequestDTO ligneDTO : dto.getLignes()) {
            Produit produit = produitRepository
                    .findByIdAndEntrepriseIdAndUserIdWithDetails(
                            ligneDTO.getProduitId(), entreprise.getId(), shopOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Produit", ligneDTO.getProduitId()));

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
    // STATUT
    // -----------------------------------------

    @Transactional
    public CommandeAchatResponseDTO changerStatut(Long id, StatutAchat nouveauStatut) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        CommandeAchat commande = findMyCommande(user, entreprise, id);

        StatutAchat actuel = commande.getStatut();
        String transition = actuel.name() + "->" + nouveauStatut.name();

        if (!TRANSITIONS_AUTORISEES.contains(transition)) {
            throw new BusinessException(String.format(
                    "Transition de statut invalide : %s → %s", actuel, nouveauStatut));
        }

        if (nouveauStatut == StatutAchat.RECUE) {
            augmenterStock(commande);
        }

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
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        CommandeAchat commande = findMyCommande(user, entreprise, id);

        if (commande.getStatut() == StatutAchat.RECUE
                || commande.getStatut() == StatutAchat.RECU_PARTIEL) {
            restituerStock(commande);
        }

        commandeAchatRepository.delete(commande);
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    private String genererNumero(Long entrepriseId, Long userId) {
        int annee = Year.now().getValue();
        long count = commandeAchatRepository.countByAnneeAndEntrepriseIdAndUserId(
                annee, entrepriseId, userId);
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

            stockFournisseurService.ajouterStock(
                    produit.getId(), fournisseurId, quantite, prixAchat);

            int ancienStock = produit.getStock();
            produit.setStock(ancienStock + quantite);
            produitRepository.save(produit);
        }
    }

    private void restituerStock(CommandeAchat commande) {
        Long fournisseurId = commande.getFournisseur().getId();
        for (LigneCommandeAchat ligne : commande.getLignes()) {
            Produit produit = ligne.getProduit();
            int quantite = ligne.getQuantite();

            try {
                stockFournisseurService.retirerStock(produit.getId(), fournisseurId, quantite);
            } catch (Exception e) {
                log.warn("Impossible de retirer du stock fournisseur : {}", e.getMessage());
            }

            int nouveauStock = produit.getStock() - quantite;
            if (nouveauStock < 0) {
                throw new BusinessException(String.format(
                        "Impossible de restituer le stock : %s deviendrait négatif",
                        produit.getNom()));
            }
            produit.setStock(nouveauStock);
            produitRepository.save(produit);
        }
    }

    private CommandeAchat findMyCommande(User user, Entreprise entreprise, Long id) {
        if (user.hasRole(Role.ROLE_ADMIN)) {
            return commandeAchatRepository
                    .findByIdAndEntrepriseIdWithDetails(id, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("CommandeAchat", id));
        }
        Long shopOwnerId = resolveShopOwnerId(user);
        return commandeAchatRepository
                .findByIdAndEntrepriseIdAndUserIdWithDetails(
                        id, entreprise.getId(), shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("CommandeAchat", id));
    }

    private Fournisseur findMyFournisseur(User user, Entreprise entreprise, Long fournisseurId) {
        if (user.hasRole(Role.ROLE_ADMIN)) {
            return fournisseurRepository
                    .findByIdAndEntrepriseIdWithDetails(fournisseurId, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", fournisseurId));
        }
        Long shopOwnerId = resolveShopOwnerId(user);
        return fournisseurRepository
                .findByIdAndEntrepriseIdAndUserIdWithDetails(
                        fournisseurId, entreprise.getId(), shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", fournisseurId));
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