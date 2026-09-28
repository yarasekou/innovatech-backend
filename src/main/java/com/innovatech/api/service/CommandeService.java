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
import com.innovatech.api.repository.ProduitRepository;
import lombok.RequiredArgsConstructor;
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
@Transactional(readOnly = true)
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final ClientRepository clientRepository;
    private final ProduitRepository produitRepository;
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
        return commandeRepository.findAllWithDetails()
                .stream()
                .map(commandeMapper::toResponseDTO)
                .toList();
    }

    public CommandeResponseDTO findById(Long id) {
        Commande commande = commandeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", id));
        return commandeMapper.toResponseDTO(commande);
    }

    public CommandeResponseDTO findByNumero(String numero) {
        Commande commande = commandeRepository.findByNumeroWithDetails(numero)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commande avec numéro " + numero + " introuvable"));
        return commandeMapper.toResponseDTO(commande);
    }

    public List<CommandeResponseDTO> findByClient(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client", clientId);
        }
        return commandeRepository.findByClientIdWithDetails(clientId)
                .stream()
                .map(commandeMapper::toResponseDTO)
                .toList();
    }

    public List<CommandeResponseDTO> findByStatut(StatutCommande statut) {
        return commandeRepository.findByStatutWithDetails(statut)
                .stream()
                .map(commandeMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // CREATE — ne déduit PAS le stock
    // -----------------------------------------

    @Transactional
    public CommandeResponseDTO create(CommandeRequestDTO dto) {
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client", dto.getClientId()));

        if (!client.isActif()) {
            throw new BusinessException("Impossible de créer une commande pour un client inactif");
        }

        Commande commande = commandeMapper.toEntity(dto, client);
        commande.setNumero(genererNumero());
        commande.setStatut(StatutCommande.EN_ATTENTE);
        commande.setDateCommande(LocalDateTime.now());

        // Ajouter les lignes (SANS toucher au stock)
        for (LigneCommandeRequestDTO ligneDTO : dto.getLignes()) {
            Produit produit = produitRepository.findById(ligneDTO.getProduitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", ligneDTO.getProduitId()));

            if (!produit.isActif()) {
                throw new BusinessException("Le produit " + produit.getReference() + " est inactif");
            }

            // Vérification simple (pas de déduction)
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

            // ⚠️ PAS de produit.setStock() ici — le stock sera déduit à la LIVRAISON

            commande.ajouterLigne(ligne);
        }

        recalculerTotaux(commande);

        Commande saved = commandeRepository.save(commande);
        return commandeMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // UPDATE — ne touche PAS au stock
    // -----------------------------------------

    @Transactional
    public CommandeResponseDTO update(Long id, CommandeRequestDTO dto) {
        Commande existing = commandeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", id));

        if (existing.getStatut() != StatutCommande.EN_ATTENTE) {
            throw new BusinessException(
                    "Seule une commande EN_ATTENTE peut être modifiée");
        }

        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client", dto.getClientId()));

        commandeMapper.updateEntity(existing, dto, client);

        // Vider les lignes (orphanRemoval les supprime)
        existing.viderLignes();

        // Recréer les nouvelles lignes (SANS toucher au stock)
        for (LigneCommandeRequestDTO ligneDTO : dto.getLignes()) {
            Produit produit = produitRepository.findById(ligneDTO.getProduitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", ligneDTO.getProduitId()));

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
    // CHANGEMENT DE STATUT — c'est ICI que le stock bouge
    // -----------------------------------------

    @Transactional
    public CommandeResponseDTO changerStatut(Long id, StatutCommande nouveauStatut) {
        Commande commande = commandeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", id));

        StatutCommande actuel = commande.getStatut();
        String transition = actuel.name() + "->" + nouveauStatut.name();

        if (!TRANSITIONS_AUTORISEES.contains(transition)) {
            throw new BusinessException(String.format(
                    "Transition de statut invalide : %s → %s", actuel, nouveauStatut));
        }

        // ✅ Avant de VALIDER : vérifier que le stock est disponible
        if (nouveauStatut == StatutCommande.VALIDEE) {
            verifierStockDisponible(commande);
        }

        // ✅ À la LIVRAISON : déduire le stock
        if (nouveauStatut == StatutCommande.LIVREE) {
            deduireStock(commande);
        }

        commande.setStatut(nouveauStatut);
        Commande updated = commandeRepository.save(commande);
        return commandeMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // DELETE — ne restitue PAS le stock (jamais déduit)
    // -----------------------------------------

    @Transactional
    public void delete(Long id) {
        Commande commande = commandeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", id));

        if (commande.getStatut() == StatutCommande.EN_COURS
                || commande.getStatut() == StatutCommande.LIVREE) {
            throw new BusinessException(
                    "Impossible de supprimer une commande en cours ou livrée. Annulez-la.");
        }

        // ⚠️ Pas de restitution de stock ici (jamais déduit)

        commandeRepository.delete(commande);
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    private String genererNumero() {
        int annee = Year.now().getValue();
        long count = commandeRepository.countByAnnee(annee);
        return String.format("CMD-%d-%04d", annee, count + 1);
    }

    private void recalculerTotaux(Commande commande) {
        BigDecimal totalHT = commande.getLignes()
                .stream()
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

    /**
     * Vérifie que le stock est disponible pour toutes les lignes.
     * Ne modifie PAS le stock.
     */
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

    /**
     * Déduit le stock pour chaque ligne de la commande.
     * Appelé uniquement lors du passage en LIVREE.
     */
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
}