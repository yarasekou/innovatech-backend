package com.innovatech.api.mapper;

import com.innovatech.api.dto.UserResponseDTO;
import com.innovatech.api.entity.User;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponseDTO toResponseDTO(User entity) {
        if (entity == null) return null;

        Set<String> roles = entity.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        UserResponseDTO.UserResponseDTOBuilder builder = UserResponseDTO.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .prenom(entity.getPrenom())
                .nom(entity.getNom())
                .roles(roles)
                .actif(entity.isActif())
                .dateCreation(entity.getDateCreation())
                .dateModification(entity.getDateModification());

        // Patron
        if (entity.getPatron() != null) {
            builder.patronId(entity.getPatron().getId())
                    .patronEmail(entity.getPatron().getEmail())
                    .patronNom(entity.getPatron().getNom())
                    .patronPrenom(entity.getPatron().getPrenom());
        }

        // Nombre d'employés
        if (entity.getEmployes() != null) {
            builder.nbEmployes(entity.getEmployes().size());
        } else {
            builder.nbEmployes(0);
        }

        return builder.build();
    }
}