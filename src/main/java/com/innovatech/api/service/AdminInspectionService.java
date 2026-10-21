package com.innovatech.api.service;

import com.innovatech.api.dto.*;
import com.innovatech.api.entity.Entreprise;
import com.innovatech.api.entity.Role;
import com.innovatech.api.entity.User;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.*;
import com.innovatech.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.innovatech.api.dto.FactureResponseDTO;

import java.util.List;

/**
 * Service d'inspection pour les SUPER_ADMIN.
 * <p>
 * Permet de voir la boutique d'un ADMIN ou d'un USER donné,
 * sans y être connecté (supervision).
 * <p>
 * ⚠️ Réservé aux SUPER_ADMIN.
 * ⚠️ Toutes les données affichées sont celles de l'ENTREPRISE
 *    du user ciblé (toutes boutiques confondues).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminInspectionService {

    private final UserRepository userRepository;
    private final CategorieRepository categorieRepository;
    private final ProduitRepository produitRepository;
    private final ClientRepository clientRepository;
    private final FournisseurRepository fournisseurRepository;
    private final CommandeRepository commandeRepository;

    private final UserMapper userMapper;
    private final CategorieMapper categorieMapper;
    private final ProduitMapper produitMapper;
    private final ClientMapper clientMapper;
    private final FournisseurMapper fournisseurMapper;
    private final CommandeMapper commandeMapper;

    private final PaiementRepository paiementRepository;
    private final PaiementMapper paiementMapper;

    private final RetourRepository retourRepository;
    private final RetourMapper retourMapper;
    private final CommandeAchatRepository commandeAchatRepository;
    private final CommandeAchatMapper commandeAchatMapper;
    private final FactureRepository factureRepository;
    private final FactureMapper factureMapper;



    // -----------------------------------------
    // Résolution du user ciblé
    // -----------------------------------------

    /**
     * Récupère le user ciblé en vérifiant :
     * - qu'il existe
     * - qu'il est ADMIN (pas USER, SELLER, SUPER_ADMIN)
     * - qu'il a une entreprise
     */
    private User resolveTargetUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.isSupprime()) {
            throw new ResourceNotFoundException("User", userId);
        }

        // ⚠️ SEULS LES ADMINS sont inspectables
        if (user.hasRole(Role.ROLE_SUPER_ADMIN)) {
            throw new BusinessException("Impossible d'inspecter un SUPER_ADMIN.");
        }
        if (user.hasRole(Role.ROLE_SELLER)) {
            throw new BusinessException(
                    "Impossible d'inspecter un SELLER. Inspectez son ADMIN.");
        }
        if (user.hasRole(Role.ROLE_USER)) {
            throw new BusinessException(
                    "Impossible d'inspecter un USER. Inspectez son ADMIN.");
        }

        // Doit être ADMIN
        if (!user.hasRole(Role.ROLE_ADMIN)) {
            throw new BusinessException(
                    "Cet utilisateur n'est pas inspectable (rôle non supporté).");
        }

        if (user.getEntreprise() == null) {
            throw new BusinessException(
                    "Cet utilisateur n'a pas d'entreprise associée.");
        }

        return user;
    }

    // -----------------------------------------
    // 1. Profil du user ciblé
    // -----------------------------------------

    public UserResponseDTO getProfil(Long userId) {
        User user = resolveTargetUser(userId);
        return userMapper.toResponseDTO(user);
    }

    // -----------------------------------------
    // 2. USERs de l'entreprise du user ciblé
    // -----------------------------------------

    public List<UserResponseDTO> getUsers(Long userId) {
        User user = resolveTargetUser(userId);

        return userRepository.findByEntrepriseIdAndRole(
                        user.getEntreprise().getId(), Role.ROLE_USER)
                .stream()
                .map(userMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // 3. Catégories de l'entreprise
    // -----------------------------------------

    public List<CategorieResponseDTO> getCategories(Long userId) {
        User user = resolveTargetUser(userId);
        Long entrepriseId = user.getEntreprise().getId();

        return categorieRepository.findByEntrepriseId(entrepriseId)
                .stream()
                .map(categorieMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // 4. Produits de l'entreprise
    // -----------------------------------------

    public List<ProduitResponseDTO> getProduits(Long userId) {
        User user = resolveTargetUser(userId);
        Long entrepriseId = user.getEntreprise().getId();

        return produitRepository.findByEntrepriseIdWithDetails(entrepriseId)
                .stream()
                .map(produitMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // 5. Clients de l'entreprise
    // -----------------------------------------

    public List<ClientResponseDTO> getClients(Long userId) {
        User user = resolveTargetUser(userId);
        Long entrepriseId = user.getEntreprise().getId();

        return clientRepository.findByEntrepriseIdWithDetails(entrepriseId)
                .stream()
                .map(clientMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // 6. Fournisseurs de l'entreprise
    // -----------------------------------------

    public List<FournisseurResponseDTO> getFournisseurs(Long userId) {
        User user = resolveTargetUser(userId);
        Long entrepriseId = user.getEntreprise().getId();

        return fournisseurRepository.findByEntrepriseIdWithDetails(entrepriseId)
                .stream()
                .map(fournisseurMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // 7. Commandes de l'entreprise
    // -----------------------------------------

    public List<CommandeResponseDTO> getCommandes(Long userId) {
        User user = resolveTargetUser(userId);
        Long entrepriseId = user.getEntreprise().getId();

        return commandeRepository.findByEntrepriseIdWithDetails(entrepriseId)
                .stream()
                .map(commandeMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // 8. Paiements de l'entreprise
    // -----------------------------------------

    public List<PaiementResponseDTO> getPaiements(Long userId) {
        User user = resolveTargetUser(userId);
        Long entrepriseId = user.getEntreprise().getId();

        return paiementRepository.findByEntrepriseIdWithDetails(entrepriseId)
                .stream()
                .map(paiementMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // 9. Retours de l'entreprise
    // -----------------------------------------

    public List<RetourResponseDTO> getRetours(Long userId) {
        User user = resolveTargetUser(userId);
        Long entrepriseId = user.getEntreprise().getId();

        return retourRepository.findByEntrepriseIdWithDetails(entrepriseId)
                .stream()
                .map(retourMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // 10. Commandes d'achat de l'entreprise
    // -----------------------------------------

    public List<CommandeAchatResponseDTO> getCommandesAchat(Long userId) {
        User user = resolveTargetUser(userId);
        Long entrepriseId = user.getEntreprise().getId();

        return commandeAchatRepository.findByEntrepriseIdWithDetails(entrepriseId)
                .stream()
                .map(commandeAchatMapper::toResponseDTO)
                .toList();
    }

    // Méthode
    public List<FactureResponseDTO> getFactures(Long userId) {
        User user = resolveTargetUser(userId);
        Long entrepriseId = user.getEntreprise().getId();

        return factureRepository.findByEntrepriseIdWithDetails(entrepriseId)
                .stream()
                .map(factureMapper::toResponseDTO)
                .toList();
    }
}