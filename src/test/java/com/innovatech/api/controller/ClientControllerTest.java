package com.innovatech.api.controller;

import tools.jackson.databind.ObjectMapper;
import com.innovatech.api.dto.ClientRequestDTO;
import com.innovatech.api.dto.ClientResponseDTO;
import com.innovatech.api.exception.ResourceNotFoundException;
import com.innovatech.api.security.CustomUserDetailsService;
import com.innovatech.api.security.JwtAuthenticationFilter;
import com.innovatech.api.security.JwtService;
import com.innovatech.api.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ClientController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ClientController - Tests MockMvc")
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClientService clientService;

    // ⚠️ Indispensable : mocker JwtAuthenticationFilter + ses dépendances
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private ClientResponseDTO responseDTO;
    private ClientRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        responseDTO = ClientResponseDTO.builder()
                .id(1L)
                .nom("Dupont")
                .prenom("Jean")
                .email("jean.dupont@example.com")
                .ville("Dakar")
                .actif(true)
                .build();

        requestDTO = ClientRequestDTO.builder()
                .nom("Dupont")
                .prenom("Jean")
                .email("jean.dupont@example.com")
                .ville("Dakar")
                .actif(true)
                .build();
    }

    @Test
    @DisplayName("GET /clients - Devrait retourner 200 et la liste")
    void shouldReturnAllClients() throws Exception {
        when(clientService.findAll()).thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/clients"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nom").value("Dupont"))
                .andExpect(jsonPath("$[0].email").value("jean.dupont@example.com"));

        verify(clientService).findAll();
    }

    @Test
    @DisplayName("GET /clients/{id} - Devrait retourner 200 si trouvé")
    void shouldReturnClientById() throws Exception {
        when(clientService.findById(1L)).thenReturn(responseDTO);

        mockMvc.perform(get("/clients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nom").value("Dupont"));
    }

    @Test
    @DisplayName("GET /clients/{id} - Devrait retourner 404 si introuvable")
    void shouldReturn404WhenNotFound() throws Exception {
        when(clientService.findById(999L))
                .thenThrow(new ResourceNotFoundException("Client", 999L));

        mockMvc.perform(get("/clients/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /clients - Devrait retourner 201 et le client créé")
    void shouldCreateClient() throws Exception {
        when(clientService.create(any(ClientRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/clients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nom").value("Dupont"));

        verify(clientService).create(any(ClientRequestDTO.class));
    }

    @Test
    @DisplayName("POST /clients - Devrait retourner 400 si validation échoue")
    void shouldReturn400WhenValidationFails() throws Exception {
        ClientRequestDTO invalidDTO = ClientRequestDTO.builder()
                .nom("")
                .prenom("J")
                .email("pas-un-email")
                .build();

        mockMvc.perform(post("/clients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verify(clientService, never()).create(any(ClientRequestDTO.class));
    }

    @Test
    @DisplayName("DELETE /clients/{id} - Devrait retourner 204")
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteClient() throws Exception {
        doNothing().when(clientService).delete(1L);

        mockMvc.perform(delete("/clients/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(clientService).delete(1L);
    }
}