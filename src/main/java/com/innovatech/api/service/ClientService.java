package com.innovatech.api.service;

import com.innovatech.api.dto.ClientRequestDTO;
import com.innovatech.api.dto.ClientResponseDTO;
import com.innovatech.api.entity.Client;
import com.innovatech.api.entity.Entreprise;
import com.innovatech.api.entity.Role;
import com.innovatech.api.entity.User;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.ClientMapper;
import com.innovatech.api.repository.ClientRepository;
import com.innovatech.api.repository.CommandeRepository;
import com.innovatech.api.repository.UserRepository;
import com.innovatech.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service de gestion des clients.
 * <p>
 * Filtrage triple :
 * <ul>
 *   <li>ADMIN  → tous les clients de l'entreprise</li>
 *   <li>USER   → seulement SA boutique</li>
 *   <li>SELLER → la boutique de son patron</li>
 * </ul>
 * <p>
 * ⚠️ Un ADMIN ne crée PAS de client.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ClientService {

    private final ClientRepository clientRepository;
    private final CommandeRepository commandeRepository;
    private final UserRepository userRepository;
    private final ClientMapper clientMapper;

    // -----------------------------------------
    // READ
    // -----------------------------------------

    public List<ClientResponseDTO> findAll() {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return clientRepository.findByEntrepriseIdWithDetails(entreprise.getId())
                    .stream()
                    .map(clientMapper::toResponseDTO)
                    .toList();
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return clientRepository.findByEntrepriseIdAndUserIdWithDetails(
                        entreprise.getId(), shopOwnerId)
                .stream()
                .map(clientMapper::toResponseDTO)
                .toList();
    }

    public List<ClientResponseDTO> findAllActifs() {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return clientRepository.findByEntrepriseIdAndActifTrueWithDetails(entreprise.getId())
                    .stream()
                    .map(clientMapper::toResponseDTO)
                    .toList();
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return clientRepository.findByEntrepriseIdAndUserIdAndActifTrueWithDetails(
                        entreprise.getId(), shopOwnerId)
                .stream()
                .map(clientMapper::toResponseDTO)
                .toList();
    }

    public ClientResponseDTO findById(Long id) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Client client;

        if (user.hasRole(Role.ROLE_ADMIN)) {
            client = clientRepository
                    .findByIdAndEntrepriseIdWithDetails(id, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client", id));
        } else {
            Long shopOwnerId = resolveShopOwnerId(user);
            client = clientRepository
                    .findByIdAndEntrepriseIdAndUserIdWithDetails(
                            id, entreprise.getId(), shopOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client", id));
        }

        return clientMapper.toResponseDTO(client);
    }

    public List<ClientResponseDTO> search(String query) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            return clientRepository.searchByEntrepriseId(entreprise.getId(), query)
                    .stream()
                    .map(clientMapper::toResponseDTO)
                    .toList();
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        return clientRepository.searchByEntrepriseIdAndUserId(
                        entreprise.getId(), shopOwnerId, query)
                .stream()
                .map(clientMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // CREATE — USER / SELLER uniquement
    // -----------------------------------------

    @Transactional
    public ClientResponseDTO create(ClientRequestDTO dto, String userEmail) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        if (user.hasRole(Role.ROLE_ADMIN)) {
            throw new BusinessException(
                    "Un ADMIN ne crée pas de clients. Créez un USER pour cela.");
        }

        Long shopOwnerId = resolveShopOwnerId(user);
        User shopOwner = userRepository.findById(shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("User (boutique)", shopOwnerId));

        if (clientRepository.existsByEmailAndEntrepriseIdAndUserId(
                dto.getEmail(), entreprise.getId(), shopOwnerId)) {
            throw new BusinessException(
                    "Vous avez déjà un client avec l'email " + dto.getEmail());
        }

        Client client = clientMapper.toEntity(dto, entreprise, shopOwner);
        Client saved = clientRepository.save(client);
        log.info("Client {} créé par {} pour la boutique de {} (entreprise: {})",
                saved.getEmail(), userEmail, shopOwner.getEmail(), entreprise.getNom());
        return clientMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @Transactional
    public ClientResponseDTO update(Long id, ClientRequestDTO dto) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Client existing = findMyClient(user, entreprise, id);

        Long shopOwnerId = existing.getUser() != null ? existing.getUser().getId() : null;
        if (shopOwnerId != null
                && !existing.getEmail().equals(dto.getEmail())
                && clientRepository.existsByEmailAndEntrepriseIdAndUserId(
                dto.getEmail(), entreprise.getId(), shopOwnerId)) {
            throw new BusinessException(
                    "Vous avez déjà un client avec l'email " + dto.getEmail());
        }

        clientMapper.updateEntity(existing, dto);
        Client updated = clientRepository.save(existing);
        return clientMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // DELETE / DEACTIVATE
    // -----------------------------------------

    @Transactional
    public void delete(Long id) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Client client = findMyClient(user, entreprise, id);

        long nbCommandes = commandeRepository.countByClientId(id);
        if (nbCommandes > 0) {
            throw new BusinessException(
                    "Impossible de supprimer ce client : " + nbCommandes
                            + " commande(s) existent. Désactivez-le à la place.");
        }

        clientRepository.delete(client);
        log.info("Client {} supprimé", client.getEmail());
    }

    @Transactional
    public void deactivate(Long id) {
        User user = getCurrentUser();
        Entreprise entreprise = getCurrentEntreprise(user);

        Client client = findMyClient(user, entreprise, id);
        client.setActif(false);
        clientRepository.save(client);
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    private Long resolveShopOwnerId(User user) {
        if (user.hasRole(Role.ROLE_SELLER)) {
            if (user.getPatron() == null) {
                throw new BusinessException("Ce SELLER n'a pas de patron assigné");
            }
            return user.getPatron().getId();
        }
        return user.getId();
    }

    private Client findMyClient(User user, Entreprise entreprise, Long id) {
        if (user.hasRole(Role.ROLE_ADMIN)) {
            return clientRepository
                    .findByIdAndEntrepriseIdWithDetails(id, entreprise.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client", id));
        }
        Long shopOwnerId = resolveShopOwnerId(user);
        return clientRepository
                .findByIdAndEntrepriseIdAndUserIdWithDetails(
                        id, entreprise.getId(), shopOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", id));
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