package com.innovatech.api.service;

import com.innovatech.api.dto.ClientRequestDTO;
import com.innovatech.api.dto.ClientResponseDTO;
import com.innovatech.api.entity.Client;
import com.innovatech.api.exception.BusinessException;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.mapper.ClientMapper;
import com.innovatech.api.repository.ClientRepository;
import com.innovatech.api.repository.CommandeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour {@link ClientService}.
 * <p>
 * On mocke le repository et le mapper pour tester UNIQUEMENT la logique métier.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClientService - Tests unitaires")
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private CommandeRepository commandeRepository;

    @Mock
    private ClientMapper clientMapper;

    @InjectMocks
    private ClientService clientService;

    private Client client;
    private ClientRequestDTO requestDTO;
    private ClientResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        client = Client.builder()
                .id(1L)
                .nom("Dupont")
                .prenom("Jean")
                .email("jean.dupont@example.com")
                .ville("Dakar")
                .actif(true)
                .dateCreation(LocalDateTime.now())
                .build();

        requestDTO = ClientRequestDTO.builder()
                .nom("Dupont")
                .prenom("Jean")
                .email("jean.dupont@example.com")
                .ville("Dakar")
                .actif(true)
                .build();

        responseDTO = ClientResponseDTO.builder()
                .id(1L)
                .nom("Dupont")
                .prenom("Jean")
                .email("jean.dupont@example.com")
                .ville("Dakar")
                .actif(true)
                .build();
    }

    // =========================================
    // FIND ALL
    // =========================================

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("Devrait retourner la liste de tous les clients")
        void shouldReturnAllClients() {
            // Arrange
            when(clientRepository.findAll()).thenReturn(List.of(client));
            when(clientMapper.toResponseDTO(client)).thenReturn(responseDTO);

            // Act
            List<ClientResponseDTO> result = clientService.findAll();

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNom()).isEqualTo("Dupont");
            verify(clientRepository, times(1)).findAll();
            verify(clientMapper, times(1)).toResponseDTO(client);
        }
    }

    // =========================================
    // FIND BY ID
    // =========================================

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("Devrait retourner le client si trouvé")
        void shouldReturnClientWhenFound() {
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(clientMapper.toResponseDTO(client)).thenReturn(responseDTO);

            ClientResponseDTO result = clientService.findById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("jean.dupont@example.com");
        }

        @Test
        @DisplayName("Devrait lever ResourceNotFoundException si non trouvé")
        void shouldThrowWhenNotFound() {
            when(clientRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.findById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Client introuvable");
        }
    }

    // =========================================
    // CREATE
    // =========================================

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("Devrait créer un client si l'email est unique")
        void shouldCreateClientWhenEmailIsUnique() {
            when(clientRepository.existsByEmail(anyString())).thenReturn(false);
            when(clientMapper.toEntity(any(ClientRequestDTO.class))).thenReturn(client);
            when(clientRepository.save(any(Client.class))).thenReturn(client);
            when(clientMapper.toResponseDTO(client)).thenReturn(responseDTO);

            ClientResponseDTO result = clientService.create(requestDTO);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("jean.dupont@example.com");
            verify(clientRepository).existsByEmail("jean.dupont@example.com");
            verify(clientRepository).save(any(Client.class));
        }

        @Test
        @DisplayName("Devrait lever BusinessException si l'email existe déjà")
        void shouldThrowWhenEmailAlreadyExists() {
            when(clientRepository.existsByEmail("jean.dupont@example.com")).thenReturn(true);

            assertThatThrownBy(() -> clientService.create(requestDTO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("existe déjà");

            verify(clientRepository, never()).save(any(Client.class));
        }
    }

    // =========================================
    // UPDATE
    // =========================================

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("Devrait mettre à jour un client existant")
        void shouldUpdateExistingClient() {
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(clientRepository.save(any(Client.class))).thenReturn(client);
            when(clientMapper.toResponseDTO(client)).thenReturn(responseDTO);

            ClientResponseDTO result = clientService.update(1L, requestDTO);

            assertThat(result).isNotNull();
            verify(clientMapper).updateEntity(any(Client.class), any(ClientRequestDTO.class));
            verify(clientRepository).save(any(Client.class));
        }

        @Test
        @DisplayName("Devrait lever exception si le client n'existe pas")
        void shouldThrowWhenUpdatingNonExistingClient() {
            when(clientRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.update(999L, requestDTO))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(clientRepository, never()).save(any(Client.class));
        }
    }

    // =========================================
    // DELETE
    // =========================================

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("Devrait supprimer un client sans commandes")
        void shouldDeleteClientWithoutOrders() {
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(commandeRepository.countByClientId(1L)).thenReturn(0L);
            // ❌ PAS de when(clientMapper.toResponseDTO(...)) ici !

            clientService.delete(1L);

            verify(clientRepository).delete(client);
        }

        @Test
        @DisplayName("Devrait refuser si le client a des commandes")
        void shouldRefuseWhenClientHasOrders() {
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(commandeRepository.countByClientId(1L)).thenReturn(3L);

            assertThatThrownBy(() -> clientService.delete(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Impossible de supprimer");

            verify(clientRepository, never()).delete(any(Client.class));
        }

        @Test
        @DisplayName("Devrait lever exception si le client n'existe pas")
        void shouldThrowWhenDeletingNonExistingClient() {
            when(clientRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.delete(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}