package com.innovatech.api.service;

import com.innovatech.api.dto.ProduitRequestDTO;
import com.innovatech.api.dto.ProduitResponseDTO;
import com.innovatech.api.entity.Produit;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.ProduitMapper;
import com.innovatech.api.repository.ProduitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProduitService {

    private final ProduitRepository produitRepository;
    private final ProduitMapper produitMapper;

    // -----------------------------------------
    // READ
    // -----------------------------------------

    public List<ProduitResponseDTO> findAll() {
        return produitRepository.findAll()
                .stream()
                .map(produitMapper::toResponseDTO)
                .toList();
    }

    public List<ProduitResponseDTO> findAllActifs() {
        return produitRepository.findByActifTrue()
                .stream()
                .map(produitMapper::toResponseDTO)
                .toList();
    }

    public ProduitResponseDTO findById(Long id) {
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        return produitMapper.toResponseDTO(produit);
    }

    public ProduitResponseDTO findByReference(String reference) {
        Produit produit = produitRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produit avec référence " + reference + " introuvable"));
        return produitMapper.toResponseDTO(produit);
    }

    public List<ProduitResponseDTO> findByCategorie(String categorie) {
        return produitRepository.findByCategorieIgnoreCase(categorie)
                .stream()
                .map(produitMapper::toResponseDTO)
                .toList();
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

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @Transactional
    public ProduitResponseDTO create(ProduitRequestDTO dto) {
        if (produitRepository.existsByReference(dto.getReference())) {
            throw new BusinessException(
                    "Un produit avec la référence " + dto.getReference() + " existe déjà");
        }
        Produit produit = produitMapper.toEntity(dto);
        Produit saved = produitRepository.save(produit);
        return produitMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @Transactional
    public ProduitResponseDTO update(Long id, ProduitRequestDTO dto) {
        Produit existing = produitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", id));

        if (!existing.getReference().equals(dto.getReference())
                && produitRepository.existsByReference(dto.getReference())) {
            throw new BusinessException(
                    "Un produit avec la référence " + dto.getReference() + " existe déjà");
        }

        produitMapper.updateEntity(existing, dto);
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
}