package com.innovatech.api.service;

import com.innovatech.api.dto.ClientRequestDTO;
import com.innovatech.api.dto.ClientResponseDTO;
import com.innovatech.api.entity.Client;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.ClientMapper;
import com.innovatech.api.repository.ClientRepository;
import com.innovatech.api.repository.CommandeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final CommandeRepository commandeRepository;

    // -----------------------------------------
    // READ
    // -----------------------------------------

    public List<ClientResponseDTO> findAll() {
        return clientRepository.findAll()
                .stream()
                .map(clientMapper::toResponseDTO)
                .toList();
    }

    public List<ClientResponseDTO> findAllActifs() {
        return clientRepository.findByActifTrue()
                .stream()
                .map(clientMapper::toResponseDTO)
                .toList();
    }

    public ClientResponseDTO findById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", id));
        return clientMapper.toResponseDTO(client);
    }

    public ClientResponseDTO findByEmail(String email) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client avec email " + email + " introuvable"));
        return clientMapper.toResponseDTO(client);
    }

    public List<ClientResponseDTO> search(String query) {
        return clientRepository
                .findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(query, query)
                .stream()
                .map(clientMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @Transactional
    public ClientResponseDTO create(ClientRequestDTO dto) {
        if (clientRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException(
                    "Un client avec l'email " + dto.getEmail() + " existe déjà");
        }
        Client client = clientMapper.toEntity(dto);
        Client saved = clientRepository.save(client);
        return clientMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @Transactional
    public ClientResponseDTO update(Long id, ClientRequestDTO dto) {
        Client existing = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", id));

        if (!existing.getEmail().equals(dto.getEmail())
                && clientRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException(
                    "Un client avec l'email " + dto.getEmail() + " existe déjà");
        }

        clientMapper.updateEntity(existing, dto);
        Client updated = clientRepository.save(existing);
        return clientMapper.toResponseDTO(updated);
    }

    @Transactional
    public void delete(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", id));

        // Vérifier s'il a des commandes
        long nbCommandes = commandeRepository.countByClientId(id);
        if (nbCommandes > 0) {
            throw new BusinessException(
                    "Impossible de supprimer ce client : " + nbCommandes
                            + " commande(s) existent. Désactivez-le à la place.");
        }

        clientRepository.delete(client);
    }

    @Transactional
    public void deactivate(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", id));
        client.setActif(false);
        clientRepository.save(client);
    }
}