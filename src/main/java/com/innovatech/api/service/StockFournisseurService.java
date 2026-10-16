package com.innovatech.api.service;

import com.innovatech.api.dto.StockFournisseurDTO;
import com.innovatech.api.entity.Entreprise;
import com.innovatech.api.entity.Fournisseur;
import com.innovatech.api.entity.Produit;
import com.innovatech.api.entity.Role;
import com.innovatech.api.entity.StockFournisseur;
import com.innovatech.api.entity.User;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.StockFournisseurMapper;
import com.innovatech.api.repository.FournisseurRepository;
import com.innovatech.api.repository.ProduitRepository;
import com.innovatech.api.repository.StockFournisseurRepository;
import com.innovatech.api.repository.UserRepository;
import com.innovatech.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StockFournisseurService {

    private final StockFournisseurRepository stockRepository;
    private final ProduitRepository produitRepository;
    private final FournisseurRepository fournisseurRepository;
    private final UserRepository userRepository;
    private final StockFournisseurMapper stockMapper;

    // -----------------------------------------
    // READ — scopé par rôle
    // -----------------------------------------

    public List<StockFournisseurDTO> findAll() {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return stockRepository.findByEntrepriseIdWithDetails(entreprise.getId())
                    .stream()
                    .map(stockMapper::toDTO)
                    .toList();
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return stockRepository.findByEntrepriseIdAndUserIdWithDetails(
                        entreprise.getId(), shopOwnerId)
                .stream()
                .map(stockMapper::toDTO)
                .toList();
    }

    public List<StockFournisseurDTO> findByProduit(Long produitId) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        // Vérifier que le produit appartient bien à l'entreprise / boutique
        findMyProduit(user, entreprise, produitId);

        return stockRepository.findByProduitIdWithDetails(produitId)
                .stream()
                .map(stockMapper::toDTO)
                .toList();
    }

    // -----------------------------------------
    // MISE À JOUR AUTOMATIQUE (appelée par CommandeAchatService)
    // -----------------------------------------

    @Transactional
    public StockFournisseur ajouterStock(Long produitId, Long fournisseurId,
                                         int quantiteAAjouter, BigDecimal prixAchat) {
        StockFournisseur stock = stockRepository
                .findByProduitIdAndFournisseurId(produitId, fournisseurId)
                .orElseGet(() -> {
                    Produit produit = produitRepository.findById(produitId)
                            .orElseThrow(() -> new ResourceNotFoundException("Produit", produitId));
                    Fournisseur fournisseur = fournisseurRepository.findById(fournisseurId)
                            .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", fournisseurId));

                    return StockFournisseur.builder()
                            .produit(produit)
                            .fournisseur(fournisseur)
                            .stock(0)
                            .prixAchat(BigDecimal.ZERO)
                            .build();
                });

        stock.setStock(stock.getStock() + quantiteAAjouter);

        if (prixAchat != null && prixAchat.compareTo(BigDecimal.ZERO) > 0) {
            stock.setPrixAchat(prixAchat);
        }

        StockFournisseur saved = stockRepository.save(stock);
        log.info("Stock fournisseur mis à jour : produit={}, fournisseur={}, stock={}, prix={}",
                produitId, fournisseurId, saved.getStock(), saved.getPrixAchat());
        return saved;
    }

    @Transactional
    public StockFournisseur retirerStock(Long produitId, Long fournisseurId, int quantiteARetirer) {
        StockFournisseur stock = stockRepository
                .findByProduitIdAndFournisseurId(produitId, fournisseurId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock fournisseur introuvable pour produit=" + produitId
                                + " et fournisseur=" + fournisseurId));

        int nouveauStock = stock.getStock() - quantiteARetirer;
        if (nouveauStock < 0) {
            throw new BusinessException(
                    "Impossible de retirer : stock deviendrait négatif ("
                            + stock.getStock() + " → " + nouveauStock + ")");
        }

        stock.setStock(nouveauStock);
        return stockRepository.save(stock);
    }

    public BigDecimal getPrixAchat(Long produitId, Long fournisseurId) {
        return stockRepository.findByProduitIdAndFournisseurId(produitId, fournisseurId)
                .map(StockFournisseur::getPrixAchat)
                .orElse(BigDecimal.ZERO);
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    private Produit findMyProduit(User user, Entreprise entreprise, Long produitId) {
        if (user.hasRole(Role.ROLE_ADMIN)) {
            return produitRepository
                    .findByIdAndEntrepriseIdWithDetails(produitId, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", produitId));
        }
        Long shopOwnerId = resolveShopOwnerId(user);
        return produitRepository
                .findByIdAndEntrepriseIdAndUserIdWithDetails(
                        produitId, entreprise.getId(), shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", produitId));
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