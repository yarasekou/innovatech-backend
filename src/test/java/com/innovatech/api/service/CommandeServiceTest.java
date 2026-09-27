package com.innovatech.api.service;

import com.innovatech.api.dto.CommandeRequestDTO;
import com.innovatech.api.dto.LigneCommandeRequestDTO;
import com.innovatech.api.entity.*;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.mapper.CommandeMapper;
import com.innovatech.api.repository.ClientRepository;
import com.innovatech.api.repository.CommandeRepository;
import com.innovatech.api.repository.ProduitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommandeService - Tests métier critiques")
class CommandeServiceTest {

    @Mock
    private CommandeRepository commandeRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ProduitRepository produitRepository;
    @Mock
    private CommandeMapper commandeMapper;

    @InjectMocks
    private CommandeService commandeService;

    private Client client;
    private Produit produit;
    private CommandeRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        client = Client.builder()
                .id(1L)
                .nom("Dupont")
                .prenom("Jean")
                .email("jean@example.com")
                .actif(true)
                .build();

        produit = Produit.builder()
                .id(1L)
                .reference("PROD-001")
                .nom("Site web")
                .prix(new BigDecimal("2500.00"))
                .stock(10)
                .actif(true)
                .build();

        LigneCommandeRequestDTO ligneDTO = LigneCommandeRequestDTO.builder()
                .produitId(1L)
                .quantite(2)
                .build();

        requestDTO = CommandeRequestDTO.builder()
                .clientId(1L)
                .tvaPercent(new BigDecimal("18.00"))
                .lignes(List.of(ligneDTO))
                .build();
    }

    @Test
    @DisplayName("Create - Devrait calculer correctement le total HT et TTC")
    void shouldComputeTotalsCorrectly() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(commandeRepository.countByAnnee(anyInt())).thenReturn(0L);
        when(commandeRepository.save(any(Commande.class))).thenAnswer(inv -> inv.getArgument(0));
        when(commandeMapper.toEntity(any(), any())).thenReturn(new Commande());
        when(commandeMapper.toResponseDTO(any())).thenReturn(null);

        commandeService.create(requestDTO);

        // Capture la commande sauvegardée
        verify(commandeRepository).save(argThat(commande -> {
            // totalHT = 2 × 2500 = 5000
            assertThat(commande.getTotalHT())
                    .isEqualByComparingTo(new BigDecimal("5000.00"));
            // totalTTC = 5000 × 1.18 = 5900
            assertThat(commande.getTotalTTC())
                    .isEqualByComparingTo(new BigDecimal("5900.00"));
            return true;
        }));
    }

    @Test
    @DisplayName("Create - Devrait lever BusinessException si stock insuffisant")
    void shouldThrowWhenStockInsufficient() {
        produit.setStock(1);   // stock insuffisant pour quantite=2
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(commandeMapper.toEntity(any(), any())).thenReturn(new Commande());

        LigneCommandeRequestDTO ligne = LigneCommandeRequestDTO.builder()
                .produitId(1L)
                .quantite(5)   // > stock=1
                .build();
        requestDTO.setLignes(List.of(ligne));

        assertThatThrownBy(() -> commandeService.create(requestDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Stock insuffisant");

        verify(commandeRepository, never()).save(any(Commande.class));
    }

    @Test
    @DisplayName("Create - Devrait décrémenter le stock du produit")
    void shouldDecrementStock() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(commandeRepository.countByAnnee(anyInt())).thenReturn(0L);
        when(commandeRepository.save(any(Commande.class))).thenAnswer(inv -> inv.getArgument(0));
        when(commandeMapper.toEntity(any(), any())).thenReturn(new Commande());
        when(commandeMapper.toResponseDTO(any())).thenReturn(null);

        commandeService.create(requestDTO);

        // Stock 10 - 2 = 8
        assertThat(produit.getStock()).isEqualTo(8);
        verify(produitRepository).save(produit);
    }

    @Test
    @DisplayName("Create - Devrait lever exception si client inactif")
    void shouldThrowWhenClientInactive() {
        client.setActif(false);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> commandeService.create(requestDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactif");

        verify(commandeRepository, never()).save(any(Commande.class));
    }

    @Test
    @DisplayName("ChangerStatut - Devrait refuser une transition invalide")
    void shouldRefuseInvalidTransition() {
        Commande commande = Commande.builder()
                .id(1L)
                .statut(StatutCommande.LIVREE)
                .build();
        when(commandeRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(commande));

        // LIVREE → EN_ATTENTE est invalide
        assertThatThrownBy(() ->
                commandeService.changerStatut(1L, StatutCommande.EN_ATTENTE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transition");

        verify(commandeRepository, never()).save(any(Commande.class));
    }
}