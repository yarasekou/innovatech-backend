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
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return retourRepository.findByEntrepriseIdWithDetails(entreprise.getId())
                    .stream().map(retourMapper::toResponseDTO).toList();
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return retourRepository.findByEntrepriseIdAndUserIdWithDetails(
                        entreprise.getId(), shopOwnerId)
                .stream().map(retourMapper::toResponseDTO).toList();
    }

    public RetourResponseDTO findById(Long id) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Retour retour;

        if (user.hasRole(Role.ROLE_ADMIN)) {
            retour = retourRepository
                    .findByIdAndEntrepriseIdWithDetails(id, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Retour", id));
        } else {
            Long shopOwnerId = resolveShopOwnerId(user);
            retour = retourRepository
                    .findByIdAndEntrepriseIdAndUserIdWithDetails(
                            id, entreprise.getId(), shopOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Retour", id));
        }

        return retourMapper.toResponseDTO(retour);
    }

    public List<RetourResponseDTO> findByStatut(StatutRetour statut) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return retourRepository.findByEntrepriseIdAndStatutWithDetails(
                            entreprise.getId(), statut)
                    .stream().map(retourMapper::toResponseDTO).toList();
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return retourRepository.findByEntrepriseIdAndUserIdAndStatutWithDetails(
                        entreprise.getId(), shopOwnerId, statut)
                .stream().map(retourMapper::toResponseDTO).toList();
    }

    public List<RetourResponseDTO> findByCommande(Long commandeId) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Commande commande = findMyCommande(user, entreprise, commandeId);
        return retourRepository.findByCommandeId(commande.getId())
                .stream().map(retourMapper::toResponseDTO).toList();
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @Transactional
    public RetourResponseDTO create(RetourRequestDTO dto, String userEmail) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            throw new BusinessException("Un ADMIN ne crée pas de retours");
        }

        Commande commande = findMyCommande(user, entreprise, dto.getCommandeId());

        if (commande.getStatut() != StatutCommande.LIVREE) {
            throw new BusinessException(
                    "Seule une commande LIVREE peut faire l'objet d'un retour");
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        User shopOwner = userRepository.findById(shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("User (boutique)", shopOwnerId));

        Retour retour = retourMapper.toEntity(
                dto, commande, commande.getClient(), shopOwner, entreprise);
        retour.setNumero(genererNumero(entreprise.getId(), shopOwnerId));
        retour.setStatut(StatutRetour.DEMANDE);
        retour.setDateDemande(LocalDateTime.now());

        for (LigneRetourRequestDTO ligneDTO : dto.getLignes()) {
            Produit produit = produitRepository.findById(ligneDTO.getProduitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", ligneDTO.getProduitId()));

            LigneCommande ligneOriginale = commande.getLignes().stream()
                    .filter(l -> l.getProduit().getId().equals(produit.getId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(String.format(
                            "Le produit '%s' n'est pas dans la commande %s",
                            produit.getNom(), commande.getNumero())));

            if (ligneDTO.getQuantite() > ligneOriginale.getQuantite()) {
                throw new BusinessException(String.format(
                        "Quantité à retourner (%d) supérieure à la commande (%d)",
                        ligneDTO.getQuantite(), ligneOriginale.getQuantite()));
            }

            LigneRetour ligne = LigneRetour.builder()
                    .produit(produit)
                    .quantite(ligneDTO.getQuantite())
                    .prixUnitaire(ligneOriginale.getPrixUnitaire())
                    .raison(ligneDTO.getRaison())
                    .build();
            ligne.calculerSousTotal();

            retour.ajouterLigne(ligne);
        }

        recalculerMontant(retour);

        Retour saved = retourRepository.save(retour);
        log.info("Retour {} créé par {} pour la commande {}",
                saved.getNumero(), userEmail, commande.getNumero());
        return retourMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // STATUT
    // -----------------------------------------

    @Transactional
    public RetourResponseDTO changerStatut(Long id, StatutRetour nouveauStatut) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Retour retour;

        if (user.hasRole(Role.ROLE_ADMIN)) {
            retour = retourRepository
                    .findByIdAndEntrepriseIdWithDetails(id, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Retour", id));
        } else {
            Long shopOwnerId = resolveShopOwnerId(user);
            retour = retourRepository
                    .findByIdAndEntrepriseIdAndUserIdWithDetails(
                            id, entreprise.getId(), shopOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Retour", id));
        }

        StatutRetour actuel = retour.getStatut();
        String transition = actuel.name() + "->" + nouveauStatut.name();

        if (!TRANSITIONS_AUTORISEES.contains(transition)) {
            throw new BusinessException(String.format(
                    "Transition de statut invalide : %s → %s", actuel, nouveauStatut));
        }

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
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Retour retour;

        if (user.hasRole(Role.ROLE_ADMIN)) {
            retour = retourRepository
                    .findByIdAndEntrepriseIdWithDetails(id, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Retour", id));
        } else {
            Long shopOwnerId = resolveShopOwnerId(user);
            retour = retourRepository
                    .findByIdAndEntrepriseIdAndUserIdWithDetails(
                            id, entreprise.getId(), shopOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Retour", id));
        }

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

    private String genererNumero(Long entrepriseId, Long userId) {
        int annee = Year.now().getValue();
        long count = retourRepository.countByAnneeAndEntrepriseIdAndUserId(
                annee, entrepriseId, userId);
        return String.format("RET-%d-%04d", annee, count + 1);
    }

    private void recalculerMontant(Retour retour) {
        BigDecimal total = retour.getLignes().stream()
                .map(LigneRetour::getSousTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        retour.setMontantTotal(total);
    }

    private void restituerStock(Retour retour) {
        for (LigneRetour ligne : retour.getLignes()) {
            Produit produit = ligne.getProduit();
            produit.setStock(produit.getStock() + ligne.getQuantite());
            produitRepository.save(produit);
        }
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