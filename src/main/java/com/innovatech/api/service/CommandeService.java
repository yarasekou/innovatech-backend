package com.innovatech.api.service;

import com.innovatech.api.dto.CommandeRequestDTO;
import com.innovatech.api.dto.CommandeResponseDTO;
import com.innovatech.api.dto.LigneCommandeRequestDTO;
import com.innovatech.api.entity.*;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.CommandeMapper;
import com.innovatech.api.repository.ClientRepository;
import com.innovatech.api.repository.CommandeRepository;
import com.innovatech.api.repository.PhotoProduitRepository;
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
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final ClientRepository clientRepository;
    private final ProduitRepository produitRepository;
    private final PhotoProduitRepository photoProduitRepository;
    private final UserRepository userRepository;
    private final CommandeMapper commandeMapper;

    private static final Set<String> TRANSITIONS_AUTORISEES = Set.of(
            "EN_ATTENTE->VALIDEE",
            "EN_ATTENTE->ANNULEE",
            "VALIDEE->EN_COURS",
            "VALIDEE->ANNULEE",
            "EN_COURS->LIVREE",
            "EN_COURS->ANNULEE"
    );

    // -----------------------------------------
    // READ
    // -----------------------------------------

    public List<CommandeResponseDTO> findAll() {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return commandeRepository.findByEntrepriseIdWithDetails(entreprise.getId())
                    .stream()
                    .map(commandeMapper::toResponseDTO)
                    .toList();
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return commandeRepository.findByEntrepriseIdAndUserIdWithDetails(
                        entreprise.getId(), shopOwnerId)
                .stream()
                .map(commandeMapper::toResponseDTO)
                .toList();
    }

    public CommandeResponseDTO findById(Long id) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Commande commande;

        if (user.hasRole(Role.ROLE_ADMIN)) {
            commande = commandeRepository
                    .findByIdAndEntrepriseIdWithDetails(id, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Commande", id));
        } else {
            Long shopOwnerId = resolveShopOwnerId(user);
            commande = commandeRepository
                    .findByIdAndEntrepriseIdAndUserIdWithDetails(
                            id, entreprise.getId(), shopOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Commande", id));
        }

        return commandeMapper.toResponseDTO(commande);
    }

    public List<CommandeResponseDTO> findByClient(Long clientId) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        // Vérifier que le client appartient à la boutique
        Client client = findMyClient(user, entreprise, clientId);

        return commandeRepository.findByClientIdWithDetails(client.getId())
                .stream()
                .map(commandeMapper::toResponseDTO)
                .toList();
    }

    public List<CommandeResponseDTO> findByStatut(StatutCommande statut) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return commandeRepository.findByEntrepriseIdAndStatutWithDetails(
                            entreprise.getId(), statut)
                    .stream()
                    .map(commandeMapper::toResponseDTO)
                    .toList();
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return commandeRepository.findByEntrepriseIdAndUserIdAndStatutWithDetails(
                        entreprise.getId(), shopOwnerId, statut)
                .stream()
                .map(commandeMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // CREATE — USER / SELLER uniquement
    // -----------------------------------------

    @Transactional
    public CommandeResponseDTO create(CommandeRequestDTO dto, String userEmail) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            throw new BusinessException(
                    "Un ADMIN ne crée pas de commandes. Créez un USER pour cela.");
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        User shopOwner = userRepository.findById(shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("User (boutique)", shopOwnerId));

        // Vérifier que le client appartient à la boutique
        Client client = findMyClient(user, entreprise, dto.getClientId());

        if (!client.isActif()) {
            throw new BusinessException("Impossible de créer une commande pour un client inactif");
        }

        // Créer la commande
        Commande commande = Commande.builder()
                .numero(genererNumero(entreprise.getId(), shopOwnerId))
                .entreprise(entreprise)
                .user(shopOwner)
                .client(client)
                .statut(StatutCommande.EN_ATTENTE)
                .tvaPercent(dto.getTvaPercent() != null ? dto.getTvaPercent() : entreprise.getTvaPercent())
                .notes(dto.getNotes())
                .dateLivraisonPrevue(dto.getDateLivraisonPrevue())
                .dateCommande(LocalDateTime.now())
                .build();

        // Ajouter les lignes
        for (LigneCommandeRequestDTO ligneDTO : dto.getLignes()) {
            // Vérifier que le produit appartient à la boutique
            Produit produit = produitRepository
                    .findByIdAndEntrepriseIdAndUserIdWithDetails(
                            ligneDTO.getProduitId(), entreprise.getId(), shopOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Produit", ligneDTO.getProduitId()));

            if (!produit.isActif()) {
                throw new BusinessException("Le produit " + produit.getReference() + " est inactif");
            }

            long nbPhotos = photoProduitRepository.countByProduitId(produit.getId());
            if (nbPhotos == 0) {
                throw new BusinessException(String.format(
                        "Le produit '%s' n'a pas de photo. Ajoutez-en une avant de le vendre.",
                        produit.getNom()));
            }

            if (produit.getStock() < ligneDTO.getQuantite()) {
                throw new BusinessException(String.format(
                        "Stock insuffisant pour %s : demandé %d, disponible %d",
                        produit.getNom(), ligneDTO.getQuantite(), produit.getStock()));
            }

            LigneCommande ligne = LigneCommande.builder()
                    .produit(produit)
                    .quantite(ligneDTO.getQuantite())
                    .prixUnitaire(produit.getPrix())
                    .build();
            ligne.calculerSousTotal();

            commande.ajouterLigne(ligne);
        }

        recalculerTotaux(commande);

        Commande saved = commandeRepository.save(commande);
        log.info("Commande {} créée par {} pour la boutique de {}",
                saved.getNumero(), userEmail, shopOwner.getEmail());
        return commandeMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @Transactional
    public CommandeResponseDTO update(Long id, CommandeRequestDTO dto) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Commande existing = findMyCommande(user, entreprise, id);

        if (existing.getStatut() != StatutCommande.EN_ATTENTE) {
            throw new BusinessException(
                    "Seule une commande EN_ATTENTE peut être modifiée");
        }

        Client client = findMyClient(user, entreprise, dto.getClientId());
        existing.setClient(client);

        if (dto.getTvaPercent() != null) {
            existing.setTvaPercent(dto.getTvaPercent());
        }
        if (dto.getNotes() != null) {
            existing.setNotes(dto.getNotes());
        }
        if (dto.getDateLivraisonPrevue() != null) {
            existing.setDateLivraisonPrevue(dto.getDateLivraisonPrevue());
        }

        existing.viderLignes();

        Long shopOwnerId = existing.getUser().getId();

        for (LigneCommandeRequestDTO ligneDTO : dto.getLignes()) {
            Produit produit = produitRepository
                    .findByIdAndEntrepriseIdAndUserIdWithDetails(
                            ligneDTO.getProduitId(), entreprise.getId(), shopOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Produit", ligneDTO.getProduitId()));

            if (!produit.isActif()) {
                throw new BusinessException("Le produit " + produit.getReference() + " est inactif");
            }

            long nbPhotos = photoProduitRepository.countByProduitId(produit.getId());
            if (nbPhotos == 0) {
                throw new BusinessException(String.format(
                        "Le produit '%s' n'a pas de photo.", produit.getNom()));
            }

            if (produit.getStock() < ligneDTO.getQuantite()) {
                throw new BusinessException(String.format(
                        "Stock insuffisant pour %s : demandé %d, disponible %d",
                        produit.getNom(), ligneDTO.getQuantite(), produit.getStock()));
            }

            LigneCommande ligne = LigneCommande.builder()
                    .produit(produit)
                    .quantite(ligneDTO.getQuantite())
                    .prixUnitaire(produit.getPrix())
                    .build();
            ligne.calculerSousTotal();

            existing.ajouterLigne(ligne);
        }

        recalculerTotaux(existing);

        Commande updated = commandeRepository.save(existing);
        return commandeMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // CHANGEMENT DE STATUT
    // -----------------------------------------

    @Transactional
    public CommandeResponseDTO changerStatut(Long id, StatutCommande nouveauStatut) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Commande commande = findMyCommande(user, entreprise, id);

        StatutCommande actuel = commande.getStatut();
        String transition = actuel.name() + "->" + nouveauStatut.name();

        if (!TRANSITIONS_AUTORISEES.contains(transition)) {
            throw new BusinessException(String.format(
                    "Transition de statut invalide : %s → %s", actuel, nouveauStatut));
        }

        if (nouveauStatut == StatutCommande.VALIDEE) {
            verifierStockDisponible(commande);
        }

        if (nouveauStatut == StatutCommande.LIVREE) {
            deduireStock(commande);
        }

        commande.setStatut(nouveauStatut);
        Commande updated = commandeRepository.save(commande);
        return commandeMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------

    @Transactional
    public void delete(Long id) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Commande commande = findMyCommande(user, entreprise, id);

        if (commande.getStatut() == StatutCommande.EN_COURS
                || commande.getStatut() == StatutCommande.LIVREE) {
            throw new BusinessException(
                    "Impossible de supprimer une commande en cours ou livrée.");
        }

        commandeRepository.delete(commande);
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    private String genererNumero(Long entrepriseId, Long userId) {
        int annee = Year.now().getValue();
        long count = commandeRepository.countByAnneeAndEntrepriseIdAndUserId(
                annee, entrepriseId, userId);
        return String.format("CMD-%d-%04d", annee, count + 1);
    }

    private void recalculerTotaux(Commande commande) {
        BigDecimal totalHT = commande.getLignes().stream()
                .map(LigneCommande::getSousTotal)
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

    private void verifierStockDisponible(Commande commande) {
        for (LigneCommande ligne : commande.getLignes()) {
            Produit produit = ligne.getProduit();
            if (produit.getStock() < ligne.getQuantite()) {
                throw new BusinessException(String.format(
                        "Stock insuffisant pour %s : demandé %d, disponible %d",
                        produit.getNom(), ligne.getQuantite(), produit.getStock()));
            }
        }
    }

    private void deduireStock(Commande commande) {
        for (LigneCommande ligne : commande.getLignes()) {
            Produit produit = ligne.getProduit();

            if (produit.getStock() < ligne.getQuantite()) {
                throw new BusinessException(String.format(
                        "Stock insuffisant pour %s : demandé %d, disponible %d",
                        produit.getNom(), ligne.getQuantite(), produit.getStock()));
            }

            produit.setStock(produit.getStock() - ligne.getQuantite());
            produitRepository.save(produit);
        }
    }

    private Commande findMyCommande(User user, Entreprise entreprise, Long id) {
        if (user.hasRole(Role.ROLE_ADMIN)) {
            return commandeRepository
                    .findByIdAndEntrepriseIdWithDetails(id, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Commande", id));
        }
        Long shopOwnerId = resolveShopOwnerId(user);
        return commandeRepository
                .findByIdAndEntrepriseIdAndUserIdWithDetails(
                        id, entreprise.getId(), shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", id));
    }

    private Client findMyClient(User user, Entreprise entreprise, Long clientId) {
        if (user.hasRole(Role.ROLE_ADMIN)) {
            return clientRepository
                    .findByIdAndEntrepriseIdWithDetails(clientId, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));
        }
        Long shopOwnerId = resolveShopOwnerId(user);
        return clientRepository
                .findByIdAndEntrepriseIdAndUserIdWithDetails(
                        clientId, entreprise.getId(), shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));
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