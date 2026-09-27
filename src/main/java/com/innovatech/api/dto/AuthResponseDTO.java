package com.innovatech.api.dto;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDTO {

    private String token;
    private String type;
    private String email;
    private String username;
    private Set<String> roles;
    private long expiresIn;
}