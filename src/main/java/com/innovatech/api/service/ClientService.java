package com.innovatech.api.service;

import com.innovatech.api.dto.ClientRequestDTO;
import com.innovatech.api.dto.ClientResponseDTO;
import com.innovatech.api.entity.Client;
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
    // READ — avec filtrage par user/seller/admin
    // -----------------------------------------

    public List<ClientResponseDTO> findAll() {
        if (SecurityUtils.isAdmin()) {
            return clientRepository.findAllWithUser()
                    .stream()
                    .map(clientMapper::toResponseDTO)
                    .toList();
        }

        Long dataOwnerId = getCurrentDataOwnerId();
        return clientRepository.findByUserIdWithUser(dataOwnerId)
                .stream()
                .map(clientMapper::toResponseDTO)
                .toList();
    }

    public List<ClientResponseDTO> findAllActifs() {
        if (SecurityUtils.isAdmin()) {
            // Pour admin : on filtre actif=true
            return clientRepository.findAllWithUser()
                    .stream()
                    .filter(Client::isActif)
                    .map(clientMapper::toResponseDTO)
                    .toList();
        }

        Long dataOwnerId = getCurrentDataOwnerId();
        return clientRepository.findByUserIdAndActifTrueWithUser(dataOwnerId)
                .stream()
                .map(clientMapper::toResponseDTO)
                .toList();
    }

    public ClientResponseDTO findById(Long id) {
        Client client;

        if (SecurityUtils.isAdmin()) {
            client = clientRepository.findByIdWithUser(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Client", id));
        } else {
            Long dataOwnerId = getCurrentDataOwnerId();
            client = clientRepository.findByIdAndUserIdWithUser(id, dataOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client", id));
        }

        return clientMapper.toResponseDTO(client);
    }

    public List<ClientResponseDTO> search(String query) {
        if (SecurityUtils.isAdmin()) {
            return clientRepository.findAllWithUser()
                    .stream()
                    .filter(c -> c.getNom().toLowerCase().contains(query.toLowerCase())
                            || c.getPrenom().toLowerCase().contains(query.toLowerCase()))
                    .map(clientMapper::toResponseDTO)
                    .toList();
        }

        Long dataOwnerId = getCurrentDataOwnerId();
        return clientRepository.searchByUserId(dataOwnerId, query)
                .stream()
                .map(clientMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // CREATE — user injecté depuis le JWT
    // -----------------------------------------

    @Transactional
    public ClientResponseDTO create(ClientRequestDTO dto, String userEmail) {
        User user = resolveUser(userEmail);
        Long ownerId = user.getId();

        if (clientRepository.existsByEmailAndUserId(dto.getEmail(), ownerId)) {
            throw new BusinessException(
                    "Vous avez déjà un client avec l'email " + dto.getEmail());
        }

        Client client = clientMapper.toEntity(dto, user);
        Client saved = clientRepository.save(client);
        log.info("Client {} créé par {}", saved.getEmail(), userEmail);
        return clientMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @Transactional
    public ClientResponseDTO update(Long id, ClientRequestDTO dto) {
        Client existing;

        if (SecurityUtils.isAdmin()) {
            existing = clientRepository.findByIdWithUser(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Client", id));
        } else {
            Long dataOwnerId = getCurrentDataOwnerId();
            existing = clientRepository.findByIdAndUserIdWithUser(id, dataOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client", id));
        }

        // Vérifier unicité email si changé
        Long ownerId = existing.getUser() != null ? existing.getUser().getId() : null;
        if (!existing.getEmail().equals(dto.getEmail())
                && ownerId != null
                && clientRepository.existsByEmailAndUserId(dto.getEmail(), ownerId)) {
            throw new BusinessException(
                    "Vous avez déjà un client avec l'email " + dto.getEmail());
        }

        clientMapper.updateEntity(existing, dto);
        Client updated = clientRepository.save(existing);
        return clientMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------

    @Transactional
    public void delete(Long id) {
        Client client;

        if (SecurityUtils.isAdmin()) {
            client = clientRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Client", id));
        } else {
            Long dataOwnerId = getCurrentDataOwnerId();
            client = clientRepository.findByIdAndUserIdWithUser(id, dataOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client", id));
        }

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
        Client client;

        if (SecurityUtils.isAdmin()) {
            client = clientRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Client", id));
        } else {
            Long dataOwnerId = getCurrentDataOwnerId();
            client = clientRepository.findByIdAndUserIdWithUser(id, dataOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client", id));
        }

        client.setActif(false);
        clientRepository.save(client);
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    private User resolveUser(String userEmail) {
        if (userEmail == null) {
            throw new BusinessException("Utilisateur non authentifié");
        }
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User introuvable avec l'email : " + userEmail));
    }

    /**
     * Retourne l'ID du propriétaire des données à filtrer.
     * <p>
     * - ADMIN  : ne devrait pas arriver ici (on filtre avant)
     * - USER   : son ID
     * - SELLER : l'ID de son patron
     */
    private Long getCurrentDataOwnerId() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null) {
            throw new BusinessException("Utilisateur non authentifié");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User introuvable avec l'email : " + email));

        // Si SELLER → retourner l'ID du patron
        if (user.getRoles().contains(Role.ROLE_SELLER)) {
            if (user.getPatron() == null) {
                throw new BusinessException(
                        "Ce SELLER n'a pas de patron assigné");
            }
            return user.getPatron().getId();
        }

        return user.getId();
    }
}