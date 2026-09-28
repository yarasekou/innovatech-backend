package com.innovatech.api.service;

import com.innovatech.api.dto.FournisseurRequestDTO;
import com.innovatech.api.dto.FournisseurResponseDTO;
import com.innovatech.api.entity.Fournisseur;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.FournisseurMapper;
import com.innovatech.api.repository.CommandeAchatRepository;
import com.innovatech.api.repository.FournisseurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FournisseurService {

    private final FournisseurRepository fournisseurRepository;
    private final FournisseurMapper fournisseurMapper;
    private final CommandeAchatRepository commandeAchatRepository;


    // -----------------------------------------
    // READ
    // -----------------------------------------

    public List<FournisseurResponseDTO> findAll() {
        return fournisseurRepository.findAll()
                .stream()
                .map(fournisseurMapper::toResponseDTO)
                .toList();
    }

    public List<FournisseurResponseDTO> findAllActifs() {
        return fournisseurRepository.findByActifTrue()
                .stream()
                .map(fournisseurMapper::toResponseDTO)
                .toList();
    }

    public FournisseurResponseDTO findById(Long id) {
        Fournisseur fournisseur = fournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id));
        return fournisseurMapper.toResponseDTO(fournisseur);
    }

    public FournisseurResponseDTO findByCode(String code) {
        Fournisseur fournisseur = fournisseurRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fournisseur avec code " + code + " introuvable"));
        return fournisseurMapper.toResponseDTO(fournisseur);
    }

    public List<FournisseurResponseDTO> findByPays(String pays) {
        return fournisseurRepository.findByPaysIgnoreCase(pays)
                .stream()
                .map(fournisseurMapper::toResponseDTO)
                .toList();
    }

    public List<FournisseurResponseDTO> search(String query) {
        return fournisseurRepository.findByNomContainingIgnoreCase(query)
                .stream()
                .map(fournisseurMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @Transactional
    public FournisseurResponseDTO create(FournisseurRequestDTO dto) {
        if (fournisseurRepository.existsByCode(dto.getCode())) {
            throw new BusinessException(
                    "Un fournisseur avec le code " + dto.getCode() + " existe déjà");
        }
        if (fournisseurRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException(
                    "Un fournisseur avec l'email " + dto.getEmail() + " existe déjà");
        }

        Fournisseur fournisseur = fournisseurMapper.toEntity(dto);
        Fournisseur saved = fournisseurRepository.save(fournisseur);
        return fournisseurMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @Transactional
    public FournisseurResponseDTO update(Long id, FournisseurRequestDTO dto) {
        Fournisseur existing = fournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id));

        if (!existing.getCode().equals(dto.getCode())
                && fournisseurRepository.existsByCode(dto.getCode())) {
            throw new BusinessException(
                    "Un fournisseur avec le code " + dto.getCode() + " existe déjà");
        }

        if (!existing.getEmail().equals(dto.getEmail())
                && fournisseurRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException(
                    "Un fournisseur avec l'email " + dto.getEmail() + " existe déjà");
        }

        fournisseurMapper.updateEntity(existing, dto);
        Fournisseur updated = fournisseurRepository.save(existing);
        return fournisseurMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------

    @Transactional
    public void delete(Long id) {
        Fournisseur fournisseur = fournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id));

        long nbCommandes = commandeAchatRepository.countByFournisseurId(id);
        if (nbCommandes > 0) {
            throw new BusinessException(
                    "Impossible de supprimer ce fournisseur : " + nbCommandes
                            + " commande(s) d'achat existent. Désactivez-le à la place.");
        }

        fournisseurRepository.delete(fournisseur);
    }

    @Transactional
    public void deactivate(Long id) {
        Fournisseur fournisseur = fournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id));
        fournisseur.setActif(false);
        fournisseurRepository.save(fournisseur);
    }
}