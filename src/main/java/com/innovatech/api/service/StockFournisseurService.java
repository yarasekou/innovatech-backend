package com.innovatech.api.service;

import com.innovatech.api.dto.StockFournisseurDTO;
import com.innovatech.api.entity.Fournisseur;
import com.innovatech.api.entity.Produit;
import com.innovatech.api.entity.StockFournisseur;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.StockFournisseurMapper;
import com.innovatech.api.repository.FournisseurRepository;
import com.innovatech.api.repository.ProduitRepository;
import com.innovatech.api.repository.StockFournisseurRepository;
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
    private final StockFournisseurMapper stockMapper;

    // -----------------------------------------
    // READ
    // -----------------------------------------

    public List<StockFournisseurDTO> findAll() {
        return stockRepository.findAllWithDetails()
                .stream()
                .map(stockMapper::toDTO)
                .toList();
    }

    public List<StockFournisseurDTO> findByProduit(Long produitId) {
        if (!produitRepository.existsById(produitId)) {
            throw new ResourceNotFoundException("Produit", produitId);
        }
        return stockRepository.findByProduitIdWithDetails(produitId)
                .stream()
                .map(stockMapper::toDTO)
                .toList();
    }

    // -----------------------------------------
    // MISE À JOUR AUTOMATIQUE (appelée par CommandeAchatService)
    // -----------------------------------------

    /**
     * Met à jour ou crée un stock fournisseur pour un produit.
     * Appelée lors de la réception d'une commande d'achat.
     *
     * @param produitId      Le produit concerné
     * @param fournisseurId  Le fournisseur
     * @param quantiteAAjouter La quantité à ajouter au stock
     * @param prixAchat      Le prix d'achat (dernier prix utilisé)
     * @return Le stock fournisseur mis à jour
     */
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

        // Incrémenter le stock
        stock.setStock(stock.getStock() + quantiteAAjouter);

        // Mettre à jour le prix (dernier prix utilisé)
        if (prixAchat != null && prixAchat.compareTo(BigDecimal.ZERO) > 0) {
            stock.setPrixAchat(prixAchat);
        }

        StockFournisseur saved = stockRepository.save(stock);
        log.info("Stock fournisseur mis à jour : produit={}, fournisseur={}, stock={}, prix={}",
                produitId, fournisseurId, saved.getStock(), saved.getPrixAchat());
        return saved;
    }

    /**
     * Retire du stock fournisseur (utilisé pour restitution).
     */
    @Transactional
    public StockFournisseur retirerStock(Long produitId, Long fournisseurId, int quantiteARetirer) {
        StockFournisseur stock = stockRepository
                .findByProduitIdAndFournisseurId(produitId, fournisseurId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock fournisseur introuvable pour produit=" + produitId
                                + " et fournisseur=" + fournisseurId));

        int nouveauStock = stock.getStock() - quantiteARetirer;
        if (nouveauStock < 0) {
            throw new com.innovatech.api.exception.BusinessException(
                    "Impossible de retirer : stock deviendrait négatif (" + stock.getStock() + " → " + nouveauStock + ")");
        }

        stock.setStock(nouveauStock);
        return stockRepository.save(stock);
    }

    /**
     * Récupère le prix d'achat chez un fournisseur pour un produit.
     */
    public BigDecimal getPrixAchat(Long produitId, Long fournisseurId) {
        return stockRepository.findByProduitIdAndFournisseurId(produitId, fournisseurId)
                .map(StockFournisseur::getPrixAchat)
                .orElse(BigDecimal.ZERO);
    }
}