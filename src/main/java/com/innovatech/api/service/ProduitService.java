package com.innovatech.api.service;

import com.innovatech.api.dto.ProduitRequestDTO;
import com.innovatech.api.dto.ProduitResponseDTO;
import com.innovatech.api.entity.Categorie;
import com.innovatech.api.entity.Produit;
import com.innovatech.api.entity.User;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.ProduitMapper;
import com.innovatech.api.repository.CategorieRepository;
import com.innovatech.api.repository.ProduitRepository;
import com.innovatech.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProduitService {

    private final ProduitRepository produitRepository;
    private final CategorieRepository categorieRepository;
    private final UserRepository userRepository;
    private final ProduitMapper produitMapper;

    // -----------------------------------------
    // READ
    // -----------------------------------------

    public List<ProduitResponseDTO> findAll() {
        return produitRepository.findAllWithDetails()
                .stream()
                .map(produitMapper::toResponseDTO)
                .toList();
    }

    public List<ProduitResponseDTO> findAllActifs() {
        return produitRepository.findByActifTrueWithDetails()
                .stream()
                .map(produitMapper::toResponseDTO)
                .toList();
    }

    public ProduitResponseDTO findById(Long id) {
        Produit produit = produitRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        return produitMapper.toResponseDTO(produit);
    }

    public ProduitResponseDTO findByReference(String reference) {
        Produit produit = produitRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produit avec référence " + reference + " introuvable"));
        return produitMapper.toResponseDTO(produit);
    }

    public List<ProduitResponseDTO> search(String query) {
        return produitRepository.findByNomContainingIgnoreCase(query)
                .stream()
                .map(produitMapper::toResponseDTO)
                .toList();
    }

    public List<ProduitResponseDTO> findStockFaible(Integer seuil) {
        return produitRepository.findByStockLessThan(seuil)
                .stream()
                .map(produitMapper::toResponseDTO)
                .toList();
    }

    public List<ProduitResponseDTO> findByCategorie(Long categorieId, boolean includeDescendants) {
        if (!categorieRepository.existsById(categorieId)) {
            throw new ResourceNotFoundException("Categorie", categorieId);
        }

        List<Produit> produits = includeDescendants
                ? produitRepository.findByCategorieAndDescendants(categorieId)
                : produitRepository.findByCategorieId(categorieId);

        return produits.stream()
                .map(produitMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @Transactional
    public ProduitResponseDTO create(ProduitRequestDTO dto, String userEmail) {
        if (produitRepository.existsByReference(dto.getReference())) {
            throw new BusinessException(
                    "Un produit avec la référence " + dto.getReference() + " existe déjà");
        }

        Categorie categorie = resolveCategorie(dto.getCategorieId());
        User user = resolveUser(userEmail);

        Produit produit = produitMapper.toEntity(dto, categorie, user);
        Produit saved = produitRepository.save(produit);
        return produitMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @Transactional
    public ProduitResponseDTO update(Long id, ProduitRequestDTO dto) {
        Produit existing = produitRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", id));

        if (!existing.getReference().equals(dto.getReference())
                && produitRepository.existsByReference(dto.getReference())) {
            throw new BusinessException(
                    "Un produit avec la référence " + dto.getReference() + " existe déjà");
        }

        Categorie categorie = resolveCategorie(dto.getCategorieId());

        produitMapper.updateEntity(existing, dto, categorie);
        Produit updated = produitRepository.save(existing);
        return produitMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------

    @Transactional
    public void delete(Long id) {
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        produitRepository.delete(produit);
    }

    @Transactional
    public void deactivate(Long id) {
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        produit.setActif(false);
        produitRepository.save(produit);
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    private Categorie resolveCategorie(Long categorieId) {
        if (categorieId == null) {
            return null;
        }
        return categorieRepository.findById(categorieId)
                .orElseThrow(() -> new ResourceNotFoundException("Categorie", categorieId));
    }

    private User resolveUser(String userEmail) {
        if (userEmail == null) {
            return null;
        }
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User avec email " + userEmail));
    }
}