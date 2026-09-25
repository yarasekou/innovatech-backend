package com.innovatech.api.service;

import com.innovatech.api.dto.CommandeRequestDTO;
import com.innovatech.api.dto.CommandeResponseDTO;
import com.innovatech.api.entity.Client;
import com.innovatech.api.entity.Commande;
import com.innovatech.api.entity.StatutCommande;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.CommandeMapper;
import com.innovatech.api.repository.ClientRepository;
import com.innovatech.api.repository.CommandeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final ClientRepository clientRepository;
    private final CommandeMapper commandeMapper;

    private static final Set<String> TRANSITIONS_AUTORISEES = Set.of(
            "EN_ATTENTE->VALIDEE",
            "EN_ATTENTE->ANNULEE",
            "VALIDEE->EN_COURS",
            "VALIDEE->ANNULEE",
            "EN_COURS->LIVREE",
            "EN_COURS->ANNULEE"
    );

    // -----------------------------------------
    // READ
    // -----------------------------------------

    public List<CommandeResponseDTO> findAll() {
        return commandeRepository.findAllWithClient()
                .stream()
                .map(commandeMapper::toResponseDTO)
                .toList();
    }

    public CommandeResponseDTO findById(Long id) {
        Commande commande = commandeRepository.findByIdWithClient(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", id));
        return commandeMapper.toResponseDTO(commande);
    }

    public CommandeResponseDTO findByNumero(String numero) {
        Commande commande = commandeRepository.findByNumeroWithClient(numero)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commande avec numéro " + numero + " introuvable"));
        return commandeMapper.toResponseDTO(commande);
    }

    public List<CommandeResponseDTO> findByClient(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client", clientId);
        }
        return commandeRepository.findByClientIdWithClient(clientId)
                .stream()
                .map(commandeMapper::toResponseDTO)
                .toList();
    }

    public List<CommandeResponseDTO> findByStatut(StatutCommande statut) {
        return commandeRepository.findByStatutWithClient(statut)
                .stream()
                .map(commandeMapper::toResponseDTO)
                .toList();
    }

    // -----------------------------------------
    // CREATE
    // -----------------------------------------

    @Transactional
    public CommandeResponseDTO create(CommandeRequestDTO dto) {
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client", dto.getClientId()));

        if (!client.isActif()) {
            throw new BusinessException("Impossible de créer une commande pour un client inactif");
        }

        Commande commande = commandeMapper.toEntity(dto, client);
        commande.setNumero(genererNumero());
        commande.setStatut(StatutCommande.EN_ATTENTE);
        commande.setDateCommande(LocalDateTime.now());
        recalculerTotaux(commande);

        Commande saved = commandeRepository.save(commande);
        return commandeMapper.toResponseDTO(saved);
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------

    @Transactional
    public CommandeResponseDTO update(Long id, CommandeRequestDTO dto) {
        Commande existing = commandeRepository.findByIdWithClient(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", id));

        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client", dto.getClientId()));

        commandeMapper.updateEntity(existing, dto, client);
        recalculerTotaux(existing);

        Commande updated = commandeRepository.save(existing);
        return commandeMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // CHANGEMENT DE STATUT
    // -----------------------------------------

    @Transactional
    public CommandeResponseDTO changerStatut(Long id, StatutCommande nouveauStatut) {
        Commande commande = commandeRepository.findByIdWithClient(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", id));

        StatutCommande actuel = commande.getStatut();
        String transition = actuel.name() + "->" + nouveauStatut.name();

        if (!TRANSITIONS_AUTORISEES.contains(transition)) {
            throw new BusinessException(String.format(
                    "Transition de statut invalide : %s → %s",
                    actuel, nouveauStatut));
        }

        commande.setStatut(nouveauStatut);
        Commande updated = commandeRepository.save(commande);
        return commandeMapper.toResponseDTO(updated);
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------

    @Transactional
    public void delete(Long id) {
        Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", id));

        if (commande.getStatut() == StatutCommande.EN_COURS
                || commande.getStatut() == StatutCommande.LIVREE) {
            throw new BusinessException(
                    "Impossible de supprimer une commande en cours ou livrée. Annulez-la.");
        }

        commandeRepository.delete(commande);
    }

    // -----------------------------------------
    // MÉTHODES PRIVÉES
    // -----------------------------------------

    private String genererNumero() {
        int annee = Year.now().getValue();
        long count = commandeRepository.countByAnnee(annee);
        return String.format("CMD-%d-%04d", annee, count + 1);
    }

    private void recalculerTotaux(Commande commande) {
        BigDecimal totalHT = BigDecimal.ZERO;
        BigDecimal tva = commande.getTvaPercent() != null
                ? commande.getTvaPercent()
                : new BigDecimal("18.00");

        BigDecimal totalTTC = totalHT
                .multiply(BigDecimal.ONE.add(tva.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);

        commande.setTotalHT(totalHT);
        commande.setTotalTTC(totalTTC);
    }
}