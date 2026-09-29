package com.innovatech.api.service;

import com.innovatech.api.dto.CategorieRequestDTO;
import com.innovatech.api.dto.CategorieResponseDTO;
import com.innovatech.api.dto.CategorieTreeDTO;
import com.innovatech.api.entity.Categorie;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.CategorieMapper;
import com.innovatech.api.repository.CategorieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategorieService {

    private final CategorieRepository categorieRepository;
    private final CategorieMapper categorieMapper;

    // -----------------------------------------
    // READ
    // -----------------------------------------

    public List<CategorieResponseDTO> findAll() {
        return categorieRepository.findAll()
                .stream()
                .map(categorieMapper::toResponseDTO)
                .toList();
    }

    public List<CategorieResponseDTO> findRacines() {
        return categorieRepository.findByParentIsNull()
                .stream()
                .map(categorieMapper::toResponseDTO)
                .toList();
    }

    public List<CategorieResponseDTO> findByParent(Long parentId) {
        if (!categorieRepository.existsById(parentId)) {
            throw new ResourceNotFoundException("Categorie", parentId);
        }
        return categorieRepository.findByParentId(parentId)
                .stream()
                .map(categorieMapper::toResponseDTO)
                .toList();
    }

    public CategorieResponseDTO findById(Long id) {
        Categorie categorie = categorieRepository.findByIdWithParent(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categorie", id));
        return categorieMapper.toResponseDTO(categorie);
    }

    public CategorieResponseDTO findBySlug(String slug) {
        Categorie categorie = categorieRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categorie avec slug " + slug + " introuvable"));
        return categorieMapper.toResponseDTO(categorie);
    }

    /**
     * Retourne l'arbre complet des catégories (racines + descendants récursifs).
     */
    public List<CategorieTreeDTO> getTree() {
        List<Categorie> racines = categorieRepository.findByParentIsNull();
        return racines.stream()
                .map(categorieMapper::toTreeDTO)
                .toList();
    }

    /**
     * Retourne l'arbre à partir d'une catégorie donnée.
     */
    public CategorieTreeDTO getTreeFrom(Long id) {
        Categorie categorie = categorieRepository.findByIdWithEnfants(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categorie", id));
        return categorieMapper.toTreeDTO(categorie);
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @Transactional
    public CategorieResponseDTO create(CategorieRequestDTO dto) {
        // 1. Résoudre le parent
        Categorie parent = null;
        if (dto.getParentId() != null) {
            parent = categorieRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categorie (parent)", dto.getParentId()));
        }

        // 2. Générer ou valider le slug
        String slug = dto.getSlug() != null && !dto.getSlug().isBlank()
                ? dto.getSlug()
                : genererSlug(dto.getNom());

        if (categorieRepository.existsBySlug(slug)) {
            throw new BusinessException("Une catégorie avec le slug '" + slug + "' existe déjà");
        }

        // 3. Calculer le niveau
        int niveau = (parent == null) ? 1 : parent.getNiveau() + 1;

        // 4. Calculer le chemin complet
        String cheminComplet = (parent == null)
                ? dto.getNom()
                : parent.getCheminComplet() + " > " + dto.getNom();

        // 5. Créer
        Categorie categorie = categorieMapper.toEntity(dto, parent, slug, niveau, cheminComplet);
        Categorie saved = categorieRepository.save(categorie);
        log.info("Catégorie créée : {} (slug: {}, niveau: {})", saved.getNom(), saved.getSlug(), saved.getNiveau());
        return categorieMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @Transactional
    public CategorieResponseDTO update(Long id, CategorieRequestDTO dto) {
        Categorie existing = categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categorie", id));

        // 1. Résoudre le parent
        Categorie parent = null;
        if (dto.getParentId() != null) {
            if (dto.getParentId().equals(id)) {
                throw new BusinessException("Une catégorie ne peut pas être son propre parent");
            }
            parent = categorieRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categorie (parent)", dto.getParentId()));
        }

        // 2. Slug
        String slug = dto.getSlug() != null && !dto.getSlug().isBlank()
                ? dto.getSlug()
                : genererSlug(dto.getNom());

        if (!existing.getSlug().equals(slug) && categorieRepository.existsBySlug(slug)) {
            throw new BusinessException("Une catégorie avec le slug '" + slug + "' existe déjà");
        }

        // 3. Niveau
        int niveau = (parent == null) ? 1 : parent.getNiveau() + 1;

        // 4. Chemin
        String cheminComplet = (parent == null)
                ? dto.getNom()
                : parent.getCheminComplet() + " > " + dto.getNom();

        categorieMapper.updateEntity(existing, dto, parent, slug, niveau, cheminComplet);
        Categorie updated = categorieRepository.save(existing);
        return categorieMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------

    @Transactional
    public void delete(Long id) {
        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categorie", id));

        // Interdire la suppression si elle a des enfants
        long nbEnfants = categorieRepository.countByParentId(id);
        if (nbEnfants > 0) {
            throw new BusinessException(
                    "Impossible de supprimer cette catégorie : elle a " + nbEnfants
                            + " sous-catégorie(s). Supprimez-les d'abord.");
        }

        // TODO plus tard : vérifier si des produits sont attachés

        categorieRepository.delete(categorie);
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    /**
     * Génère un slug depuis un nom :
     * "Google Pixel 10 Pro" → "google-pixel-10-pro"
     */
    private String genererSlug(String nom) {
        if (nom == null) return "categorie";

        // Supprimer les accents
        String normalized = Normalizer.normalize(nom, Normalizer.Form.NFD);
        String sansAccents = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        return sansAccents
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")   // garder lettres, chiffres, espaces, tirets
                .replaceAll("\\s+", "-")            // espaces → tirets
                .replaceAll("-+", "-");             // tirets multiples → simple
    }

    /**
     * Liste toutes les catégories feuilles (utilisables pour un produit).
     */
    public List<CategorieResponseDTO> findFeuilles() {
        return categorieRepository.findFeuilles()
                .stream()
                .map(categorieMapper::toResponseDTO)
                .toList();
    }

    /**
     * Liste les feuilles descendantes d'une catégorie donnée.
     */
    public List<CategorieResponseDTO> findFeuillesDescendants(Long categorieId) {
        Categorie categorie = categorieRepository.findById(categorieId)
                .orElseThrow(() -> new ResourceNotFoundException("Categorie", categorieId));

        return categorieRepository.findFeuillesDescendants(categorieId, categorie.getCheminComplet())
                .stream()
                .map(categorieMapper::toResponseDTO)
                .toList();
    }
}